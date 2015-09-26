package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.services.humidity.HumidityService;
import com.dehnes.rest.demo.services.temperature.TemperatureHandleService;
import com.dehnes.rest.demo.utils.ByteTools;
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
    private final TemperatureHandleService temperatureHandleService;
    private final HumidityService humidityService;
    private final InfluxDBConnector influxDBConnector;

    public SensorReceiverService(SerialConnection serialConnection, SensorRepo sensorRepo, TemperatureHandleService temperatureHandleService, HumidityService humidityService, InfluxDBConnector influxDBConnector) {
        this.serialConnection = serialConnection;
        this.sensorRepo = sensorRepo;
        this.temperatureHandleService = temperatureHandleService;
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

        Optional<Integer> temp = Optional.empty();
        Optional<Integer> humidity = Optional.empty();
        Optional<Integer> counter = Optional.empty();
        Optional<Integer> light = Optional.empty();

        if (sensorDef.getVersion().getTemperaturePos().isPresent()) {
            temp = temperatureHandleService.extractTemperature(sensorDef.getVersion().getTemperaturePos().get(), packet, sensorDef);
            if (temp.isPresent()) {
                logger.info(sensorDef.getName() + " - temperature: " + temp.get());

                if (sensorDef.getVersion().getHumidityPos().isPresent()) {
                    humidity = humidityService.extractHumdity(sensorDef.getVersion().getHumidityPos().get(), packet, temp.get());
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
            counter = Optional.of(packet.getMessage()[sensorDef.getVersion().getCounterPos().get()]);
        }
        if (sensorDef.getVersion().getLightPos().isPresent()) {
            int lightLow = packet.getMessage()[sensorDef.getVersion().getLightPos().get()];
            int lightHi = packet.getMessage()[sensorDef.getVersion().getLightPos().get() + 1];
            light = Optional.of(ByteTools.merge(lightLow, lightHi));
        }

        // record received data in db
        influxDBConnector.recordSensorData(sensorDef, temp, humidity, counter, light);
    }

}
