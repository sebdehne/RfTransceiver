package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.services.humidity.HumidityService;
import com.dehnes.rest.demo.utils.ByteTools;
import com.dehnes.rest.demo.utils.MathTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.function.Consumer;

public class SensorReceiverService {

    private static final Logger logger = LoggerFactory.getLogger(SensorReceiverService.class);

    private final SensorRepo sensorRepo;
    private final SerialConnection serialConnection;
    private final Consumer<SerialConnection.RfPacket> listener;
    private final HumidityService humidityService;
    private final InfluxDBConnector influxDBConnector;

    public SensorReceiverService(SerialConnection serialConnection, SensorRepo sensorRepo, HumidityService humidityService, InfluxDBConnector influxDBConnector) {
        this.serialConnection = serialConnection;
        this.sensorRepo = sensorRepo;
        this.humidityService = humidityService;
        this.influxDBConnector = influxDBConnector;
        listener = this::handleIncoming;
    }

    @PostConstruct
    public void start() {
        serialConnection.registerListener(listener);
    }

    @PreDestroy
    public void stop() {
        serialConnection.unregisterListener(listener);
    }

    private void handleIncoming(SerialConnection.RfPacket packet) {
        int sensorId = packet.getRemoteAddr();
        SensorRepo.SensorDef sensorDef = sensorRepo.getSensorDef(sensorId);

        if (sensorDef == null) {
            logger.debug("Received message from unknown sensor " + packet.getRemoteAddr());
            return;
        }

        Optional<String> temp = Optional.empty();
        Optional<String> humidity = Optional.empty();
        Optional<String> counter = Optional.empty();
        Optional<String> light = Optional.empty();
        Optional<String> batVolt = Optional.empty();

        if (sensorDef.getVersion().getTemperaturePos().isPresent()) {
            Optional<Integer> temperature = sensorDef.getThermistorConfig().tempValueToResistence(
                    getAdcValue(packet, sensorDef.getVersion().getTemperaturePos().get()));
            temp = temperature.map(t -> MathTools.divideBy100(sensorId));

            if (temp.isPresent()) {
                logger.info(sensorDef.getName() + " - temperature: " + temp.get());

                if (sensorDef.getVersion().getHumidityPos().isPresent()) {
                    humidity = humidityService.extractHumdity(sensorDef.getVersion().getHumidityPos().get(), packet, temperature.get());
                    if (humidity.isPresent()) {
                        logger.info(sensorDef.getName() + " - relative humidity: " + humidity.get());
                    } else {
                        logger.warn("Failed to calc humidity");
                    }
                }
            } else {
                logger.warn("Failed to calc temp");
            }
        }
        if (sensorDef.getVersion().getCounterPos().isPresent()) {
            counter = Optional.of(String.valueOf(packet.getMessage()[sensorDef.getVersion().getCounterPos().get()]));
        }
        if (sensorDef.getVersion().getLightPos().isPresent()) {
            light = Optional.of(String.valueOf(getAdcValue(packet, sensorDef.getVersion().getLightPos().get())));
        }

        if (sensorDef.getVersion().getBatteryVoltPos().isPresent()) {
            batVolt = Optional.of(MathTools.divideBy100(calcVoltage(getAdcValue(packet, sensorDef.getVersion().getBatteryVoltPos().get()))));
        }

        // record received data in db
        influxDBConnector.recordSensorData(sensorDef, temp, humidity, counter, light, batVolt);
    }

    private int getAdcValue(SerialConnection.RfPacket packet, Integer pos) {
        int valueLow = packet.getMessage()[pos];
        int valueHigh = packet.getMessage()[pos + 1];
        return ByteTools.merge(valueLow, valueHigh);
    }

    private int calcVoltage(int adcValue) {
        return ((102300 / adcValue) * 6 ) / 10;
    }


}
