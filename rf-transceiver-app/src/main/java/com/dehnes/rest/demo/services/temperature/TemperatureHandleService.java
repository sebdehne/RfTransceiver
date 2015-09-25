package com.dehnes.rest.demo.services.temperature;

import com.dehnes.rest.demo.client.SerialConnection;
import com.dehnes.rest.demo.services.SensorRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.function.Consumer;

public class TemperatureHandleService {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureHandleService.class);

    private final SensorRepo sensorRepo;
    private final SerialConnection serialConnection;
    private final Consumer<SerialConnection.RfPacket> listener;

    public TemperatureHandleService(SensorRepo sensorRepo, SerialConnection serialConnection) {
        this.sensorRepo = sensorRepo;
        this.serialConnection = serialConnection;
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

        Optional<Integer> temperaturePosition = sensorDef.getVersion().getTemperaturePos();
        if (!temperaturePosition.isPresent()) {
            logger.debug("Sensor " + sensorId + " has no temperature support");
        } else {
            int tempLow = packet.getMessage()[temperaturePosition.get()];
            int tempHi = packet.getMessage()[temperaturePosition.get() + 1];
            int tempValue = merge(tempLow, tempHi);

            logger.info("===> Temperature in " + sensorDef.getName() + " is " + sensorDef.getThermistorConfig().tempValueToResistence(tempValue).get() + " C");
        }
    }

    private int merge(int low, int hi) {
        hi = hi << 8;
        return hi + low;
    }
}
