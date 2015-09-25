package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.client.SerialConnection;
import com.dehnes.rest.demo.services.humidity.HumidityService;
import com.dehnes.rest.demo.services.temperature.TemperatureHandleService;
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

    public SensorReceiverService(SerialConnection serialConnection, SensorRepo sensorRepo, TemperatureHandleService temperatureHandleService, HumidityService humidityService) {
        this.serialConnection = serialConnection;
        this.sensorRepo = sensorRepo;
        this.temperatureHandleService = temperatureHandleService;
        this.humidityService = humidityService;
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
            logger.warn("Received message from unknown sensor " + packet.getRemoteAddr());
            return;
        }

        if (sensorDef.getVersion().getTemperaturePos().isPresent()) {
            Optional<Integer> temp = temperatureHandleService.extractTemperature(sensorDef.getVersion().getTemperaturePos().get(), packet, sensorDef);
            if (temp.isPresent()) {
                logger.info(sensorDef.getName() + " - temperature: " + temp.get());

                if (sensorDef.getVersion().getHumidityPos().isPresent()) {
                    Optional<Integer> humdity = humidityService.extractHumdity(sensorDef.getVersion().getHumidityPos().get(), packet, temp.get());
                    if (humdity.isPresent()) {
                        logger.info(sensorDef.getName() + " - relative humidity: " + humdity.get());
                    } else {
                        logger.warn("Failed to calc humidity");
                    }
                }
            } else {
                logger.warn("Failed to calc temp");
            }
        }


    }

}
