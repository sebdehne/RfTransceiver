package com.dehnes.rest.demo.services.chipcap2_sensors;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.utils.ByteTools;
import com.dehnes.rest.demo.utils.MathTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ChipCap2SensorService {
    private static final Logger logger = LoggerFactory.getLogger(ChipCap2SensorService.class);

    private final SerialConnection serialConnection;
    private final InfluxDBConnector influxDBConnector;
    private final Function<SerialConnection.RfPacket, Boolean> listener;
    private final Map<Integer, String> sensorRepo;

    public ChipCap2SensorService(InfluxDBConnector influxDBConnector, SerialConnection serialConnection) {
        this.influxDBConnector = influxDBConnector;
        this.serialConnection = serialConnection;
        this.listener = this::handleIncoming;
        this.sensorRepo = Collections.unmodifiableMap(new HashMap<Integer, String>() {{
            put(6, "test-sensor");
        }});
    }

    @PostConstruct
    public void start() {
        serialConnection.registerListener(listener);
    }

    @PreDestroy
    public void stop() {
        serialConnection.unregisterListener(listener);
    }


    private boolean handleIncoming(SerialConnection.RfPacket rfPacket) {
        String name = sensorRepo.get(rfPacket.getRemoteAddr());
        if (name == null) {
            return false;
        }


        logger.info("Relative humidity " + MathTools.divideBy100(getRelativeHumidity(rfPacket)));
        logger.info("Temperature " + MathTools.divideBy100(getTemperature(rfPacket)));


        return true;
    }

    private int getTemperature(SerialConnection.RfPacket packet) {
        return (int) ((((((float) ByteTools.merge(packet.getMessage()[3], packet.getMessage()[2])) / 16384F) * 165) - 40) * 100);
    }

    private int getRelativeHumidity(SerialConnection.RfPacket packet) {
        return (int) ((((float) ByteTools.merge(packet.getMessage()[1], packet.getMessage()[0])) / 16384F) * 100 * 100);
    }
}
