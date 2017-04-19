package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.utils.MathTools;
import com.dehnes.rest.demo.utils.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class HeatingControllerService {
    private static final Logger logger = LoggerFactory.getLogger(HeatingControllerService.class);
    private static final String TARGET_TEMP_KEY = "HeatingControllerService.targetTemp";
    private static final String HEATER_STATUS_KEY = "HeatingControllerService.heaterTarget";
    private static final String OPERATING_MODE = "HeatingControllerService.operatingMode";

    private static final int COMMAND_READ_STATUS = 1;
    private static final int COMMAND_SWITCH_ON_HEATER = 2;
    private static final int COMMAND_SWITCH_OFF_HEATER = 3;

    private static final long holdOffInMillis = TimeUnit.MINUTES.toMillis(10); // no need to switch more often
    //private static final long holdOffInMillis = TimeUnit.MINUTES.toMillis(0); // no need to switch more often

    private static final int senderId = 27;
    private static final int maxRetries = 10;

    private final AtomicInteger failedAttempts = new AtomicInteger();

    private long lastSwitchedTimestamp = 0;

    private final PersistenceService persistenceService;
    private final ScheduledExecutorService timer;
    private final CommandSender commandSender;
    private final SerialConnection serialConnection;
    private final InfluxDBConnector influxDBConnector;
    private final LinkedBlockingQueue<SerialConnection.RfPacket> received = new LinkedBlockingQueue<>();

    private volatile ScheduledFuture<?> task;

    public HeatingControllerService(
            PersistenceService persistenceService,
            CommandSender commandSender,
            SerialConnection serialConnection,
            InfluxDBConnector influxDBConnector) {

        this.persistenceService = persistenceService;
        this.commandSender = commandSender;
        this.serialConnection = serialConnection;
        this.influxDBConnector = influxDBConnector;
        this.timer = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void start() {
        if (task != null) {
            throw new RuntimeException("Already started");
        }
        serialConnection.registerListener(this::handleIncoming);
        task = timer.scheduleWithFixedDelay(() -> {
            try {
                tick();
            } catch (Exception e) {
                logger.error("", e);
            }
        }, 0, 2, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() {
        if (task == null) {
            throw new RuntimeException("Already stopped");
        }

        task.cancel(false);
        task = null;
        serialConnection.unregisterListener(this::handleIncoming);
    }

    public synchronized String getConfiguredHeaterTarget() {
        return persistenceService.get(HEATER_STATUS_KEY, "off");
    }

    public synchronized boolean switchOn() {
        logger.info("Moving to permanent mode: on");
        persistenceService.set(OPERATING_MODE, Mode.ON.name());
        return tick();
    }

    public synchronized boolean switchOff() {
        logger.info("Moving to permanent mode: off");
        persistenceService.set(OPERATING_MODE, Mode.OFF.name());
        return tick();
    }

    public synchronized boolean manualMode(int targetTemp) {
        logger.info("Moving to mode: manual (target: " + MathTools.divideBy100(targetTemp) + ")");
        lastSwitchedTimestamp = 0;
        persistenceService.set(OPERATING_MODE, Mode.MANUAL.name());
        persistenceService.set(TARGET_TEMP_KEY, String.valueOf(targetTemp));
        return tick();
    }

    public synchronized boolean automaticMode() {
        logger.info("Moving to mode: automatic");
        lastSwitchedTimestamp = 0;
        persistenceService.set(OPERATING_MODE, Mode.AUTOMATIC.name());
        return tick();
    }

    public synchronized Mode getCurrentMode() {
        return Mode.valueOf(persistenceService.get(OPERATING_MODE, Mode.AUTOMATIC.name()));
    }

    public synchronized int getTargetTemperature() {
        return Integer.valueOf(persistenceService.get(TARGET_TEMP_KEY, String.valueOf(25 * 100)));
    }

    private void setTargetTemperatureInternal(int targetTemp) {
        persistenceService.set(TARGET_TEMP_KEY, String.valueOf(targetTemp));
    }

    private synchronized boolean tick() {
        logger.debug("tick()");

        Mode currentMode = getCurrentMode();
        logger.info("Current mode: " + currentMode);

        if (currentMode == Mode.AUTOMATIC) {
            adjustTargetTemperature();
        }

        // request measurement
        SerialConnection.RfPacket rfPacket = sendWithRetries(COMMAND_READ_STATUS);
        logger.debug("got - " + rfPacket);
        // report measurements to influxDb, including operationMode and targetTemp
        Tuple<Integer, Boolean> tuple = reportValues(rfPacket, currentMode, failedAttempts.get());
        if (tuple == null) {
            logger.warn("Did not receive anything from heater controller, giving up");
            return false;
        }

        if (currentMode == Mode.AUTOMATIC || currentMode == Mode.MANUAL) {
            if ((lastSwitchedTimestamp + holdOffInMillis) >= System.currentTimeMillis()) {
                logger.debug("Waiting for holdOff periode");
            } else {
                int targetTemperature = getTargetTemperature();
                logger.debug("Evaluating target temperature now - " + targetTemperature);
                if (tuple.a < targetTemperature) {
                    logger.debug("Setting heater to on");
                    persistenceService.set(HEATER_STATUS_KEY, "on");
                    lastSwitchedTimestamp = System.currentTimeMillis();
                } else {
                    logger.debug("Setting heater to off");
                    persistenceService.set(HEATER_STATUS_KEY, "off");
                    lastSwitchedTimestamp = System.currentTimeMillis();
                }
            }
        } else if (currentMode == Mode.OFF) {
            persistenceService.set(HEATER_STATUS_KEY, "off");
        } else if (currentMode == Mode.ON) {
            persistenceService.set(HEATER_STATUS_KEY, "on");
        }

        // bring the heater to the desired state
        if (tuple.b && "off".equals(getConfiguredHeaterTarget())) {
            return sendWithRetries(COMMAND_SWITCH_OFF_HEATER) != null;
        } else if (!tuple.b && "on".equals(getConfiguredHeaterTarget())) {
            return sendWithRetries(COMMAND_SWITCH_ON_HEATER) != null;
        }
        return true;
    }

    private void adjustTargetTemperature() {

        Optional<Integer> west = getTemp("out-west");
        Optional<Integer> east = getTemp("out-east");

        int currentTemp;
        if (west.isPresent() && east.isPresent()) {
            currentTemp = Math.min(west.get(), east.get());
        } else if (west.isPresent()) {
            currentTemp = west.get();
        } else if (east.isPresent()) {
            currentTemp = east.get();
        } else {
            logger.warn("Forced to give up adjusting target temp, no outside temp available");
            return;
        }

        // calc
        final float factor = -1.2F;
        int targetTemp = (int) (((float) currentTemp) * factor + 4000);
        // limit by max values 22,45
        targetTemp = Math.min(targetTemp, 4500);
        targetTemp = Math.max(targetTemp, 0);
        logger.info("Current coldest outside is " + currentTemp);
        logger.info("Adjusting target to " + targetTemp);
        setTargetTemperatureInternal(targetTemp);
    }

    private Optional<Integer> getTemp(String room) {
        try {
            return Optional.of((int) (influxDBConnector.avgTempDuringLastHour(room)
                    .getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONArray("series")
                    .optJSONObject(0)
                    .getJSONArray("values")
                    .getJSONArray(0)
                    .getDouble(1) * 100));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private SerialConnection.RfPacket sendWithRetries(int command) {
        if (failedAttempts.get() > 10) {
            logger.warn("Overriding command " + command + " with OFF because of too many failed attempts " + failedAttempts.get());
            command = COMMAND_SWITCH_OFF_HEATER;
        }
        received.clear();
        int retryCounter = 0;
        while (retryCounter < maxRetries) {
            commandSender.sendValue(senderId, command, false);

            SerialConnection.RfPacket packet = null;
            try {
                packet = received.poll(1, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }

            if (packet != null) {
                failedAttempts.set(0);
                return packet;
            }

            retryCounter++;

        }
        logger.warn("Giving up sending command " + command + " to heater controller");
        failedAttempts.incrementAndGet();
        return null;
    }

    private boolean handleIncoming(SerialConnection.RfPacket p) {
        if (p.getRemoteAddr() != senderId) {
            return false;
        }

        received.offer(p);

        return true;
    }

    private Tuple<Integer, Boolean> reportValues(SerialConnection.RfPacket p, Mode currentMode, int failedAttempts) {
        if (p != null) {
            int temperature = Sht15SensorService.getTemperature(p);
            String temp = MathTools.divideBy100(temperature);
            String humidity = MathTools.divideBy100(Sht15SensorService.getRelativeHumidity(p, temperature));
            boolean heaterStatus = p.getMessage()[4] == 1;

            logger.info("Relative humidity " + humidity);
            logger.info("Temperature " + temp);
            logger.info("Heater on? " + heaterStatus);

            if (temperature >= -4000 && temperature <= 8000) {
                influxDBConnector.recordSensorData(
                        "sensor",
                        Optional.of(new InfluxDBConnector.KeyValue("room", "heating_controller")),
                        Arrays.asList(
                                new InfluxDBConnector.KeyValue("temperature", temp),
                                new InfluxDBConnector.KeyValue("humidity", humidity),
                                new InfluxDBConnector.KeyValue("heater_status", String.valueOf((heaterStatus ? 1 : 0))),
                                new InfluxDBConnector.KeyValue("automatic_mode", String.valueOf(currentMode == Mode.AUTOMATIC ? 1 : 0)),
                                new InfluxDBConnector.KeyValue("manual_mode", String.valueOf(currentMode == Mode.MANUAL ? 1 : 0)),
                                new InfluxDBConnector.KeyValue("target_temperature", String.valueOf(MathTools.divideBy100(getTargetTemperature()))),
                                new InfluxDBConnector.KeyValue("configured_heater_target", String.valueOf(getConfiguredHeaterTarget().equals("on") ? 1 : 0)),
                                new InfluxDBConnector.KeyValue("failed_attempts", String.valueOf(failedAttempts))
                        )
                );
                return new Tuple<>(temperature, heaterStatus);
            } else {
                logger.info("Ignoring abnormal values " + temperature);
            }
        }

        influxDBConnector.recordSensorData(
                "sensor",
                Optional.of(new InfluxDBConnector.KeyValue("room", "heating_controller")),
                Arrays.asList(
                        new InfluxDBConnector.KeyValue("automatic_mode", String.valueOf(currentMode == Mode.AUTOMATIC ? 1 : 0)),
                        new InfluxDBConnector.KeyValue("manual_mode", String.valueOf(currentMode == Mode.MANUAL ? 1 : 0)),
                        new InfluxDBConnector.KeyValue("target_temperature", String.valueOf(MathTools.divideBy100(getTargetTemperature()))),
                        new InfluxDBConnector.KeyValue("configured_heater_target", String.valueOf(getConfiguredHeaterTarget().equals("on") ? 1 : 0)),
                        new InfluxDBConnector.KeyValue("failed_attempts", String.valueOf(failedAttempts))
                )
        );
        return null;

    }

    public enum Mode {
        ON,
        OFF,
        AUTOMATIC,
        MANUAL
    }

}
