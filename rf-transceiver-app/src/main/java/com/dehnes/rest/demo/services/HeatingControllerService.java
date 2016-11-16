package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.services.chipcap2_sensors.ChipCap2SensorService;
import com.dehnes.rest.demo.utils.MathTools;
import com.dehnes.rest.demo.utils.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.*;

@SuppressWarnings("WeakerAccess")
public class HeatingControllerService {
    private static final Logger logger = LoggerFactory.getLogger(HeatingControllerService.class);
    private static final String TARGET_TEMP_KEY = "HeatingControllerService.targetTemp";
    private static final String AUTOMATIC_MODE_KEY = "HeatingControllerService.automaticMode";
    private static final String HEATER_STATUS_KEY = "HeatingControllerService.heaterTarget";

    private static final int COMMAND_READ_STATUS = 1;
    private static final int COMMAND_SWITCH_ON_HEATER = 2;
    private static final int COMMAND_SWITCH_OFF_HEATER = 2;

    private static final long holdOffInMillis = TimeUnit.MINUTES.toMillis(10); // no need to switch more often

    private static final int senderId = 27;
    private static final int maxRetries = 10;

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

    private synchronized void tick() {
        // request measurement
        SerialConnection.RfPacket rfPacket = sendWithRetries(COMMAND_READ_STATUS);

        // report measurements to influxDb, including operationMode and targetTemp
        Tuple<Integer, Boolean> tuple = reportValues(rfPacket);

        if (isInAutomaticMode()) {
            if ((lastSwitchedTimestamp + holdOffInMillis) < System.currentTimeMillis()) {
                if (tuple.a < getTargetTemperature()) {
                    persistenceService.set(HEATER_STATUS_KEY, "on");
                    lastSwitchedTimestamp = System.currentTimeMillis();
                } else {
                    persistenceService.set(HEATER_STATUS_KEY, "off");
                    lastSwitchedTimestamp = System.currentTimeMillis();
                }
            }
        }

        // bring the heater to the desired state
        if (tuple.b && "off".equals(getConfiguredHeaterTarget())) {
            switchOff();
        } else if (!tuple.b && "on".equals(getConfiguredHeaterTarget())) {
            switchOn();
        }
    }

    public synchronized String getConfiguredHeaterTarget() {
        return persistenceService.get(HEATER_STATUS_KEY, "off");
    }

    public synchronized void switchOn() {
        persistenceService.set(HEATER_STATUS_KEY, "on");
        sendWithRetries(COMMAND_SWITCH_ON_HEATER);
    }

    public synchronized void switchOff() {
        persistenceService.set(HEATER_STATUS_KEY, "off");
        sendWithRetries(COMMAND_SWITCH_OFF_HEATER);
    }

    private SerialConnection.RfPacket sendWithRetries(int command) {
        int retryCounter = 0;
        while (retryCounter < maxRetries) {
            commandSender.sendValue(senderId, command, false);

            SerialConnection.RfPacket packet = null;
            try {
                packet = received.poll(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }

            if (packet != null) {
                return packet;
            }

            retryCounter++;

        }
        return null;
    }

    public synchronized void setAutomaticMode(boolean automaticMode) {
        persistenceService.set(AUTOMATIC_MODE_KEY, String.valueOf(automaticMode));
    }

    public synchronized boolean isInAutomaticMode() {
        return Boolean.parseBoolean(persistenceService.get(AUTOMATIC_MODE_KEY, String.valueOf(Boolean.TRUE)));
    }

    public synchronized void setTargetTemperature(int temperature) {
        persistenceService.set(TARGET_TEMP_KEY, String.valueOf(temperature));
    }

    public synchronized int getTargetTemperature() {
        return Integer.valueOf(persistenceService.get(TARGET_TEMP_KEY, String.valueOf(25 * 100)));
    }

    private boolean handleIncoming(SerialConnection.RfPacket p) {
        if (p.getRemoteAddr() != senderId) {
            return false;
        }

        received.offer(p);

        return true;
    }

    private Tuple<Integer, Boolean> reportValues(SerialConnection.RfPacket p) {
        int temperature = ChipCap2SensorService.getTemperature(p);
        String temp = MathTools.divideBy100(temperature);
        String humidity = MathTools.divideBy100(ChipCap2SensorService.getRelativeHumidity(p));
        boolean heaterStatus = p.getMessage()[4] == 1;

        logger.info("Relative humidity " + humidity);
        logger.info("Temperature " + temp);
        logger.info("Heater on? " + heaterStatus);

        influxDBConnector.recordSensorData(
                "sensor",
                Optional.of(new InfluxDBConnector.KeyValue("room", "heating_controller")),
                Arrays.asList(
                        new InfluxDBConnector.KeyValue("temperature", temp),
                        new InfluxDBConnector.KeyValue("humidity", humidity),
                        new InfluxDBConnector.KeyValue("heater_status", String.valueOf((heaterStatus ? 1 : 0))),
                        new InfluxDBConnector.KeyValue("automatic_mode", String.valueOf(isInAutomaticMode() ? 1 : 0)),
                        new InfluxDBConnector.KeyValue("target_temperature", String.valueOf(MathTools.divideBy100(getTargetTemperature()))),
                        new InfluxDBConnector.KeyValue("configured_heater_target", String.valueOf(getConfiguredHeaterTarget().equals("on") ? 1 : 0))
                )
        );

        return new Tuple<>(temperature, heaterStatus);
    }

}
