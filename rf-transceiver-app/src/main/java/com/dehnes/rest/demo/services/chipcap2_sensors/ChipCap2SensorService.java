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
import java.util.Optional;
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
            put(2, "bath");
            put(3, "storage");
            put(4, "out-west");
            put(5, "out-east");
            put(6, "test-sensor");
            put(7, "tv_room");
            put(8, "bath_kids"); // was living_room
            put(9, "hallway_down");
            put(10, "sleeping_room");
            put(11, "under_floor"); // was kitchen
            put(12, "mynthe_room");
            put(13, "noan_room");
            put(14, "garage");
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


    private boolean handleIncoming(SerialConnection.RfPacket p) {
        String name = sensorRepo.get(p.getRemoteAddr());
        if (name == null) {
            return false;
        }

        String temp = MathTools.divideBy100(getTemperature(p));
        String humidity = MathTools.divideBy100(getRelativeHumidity(p));
        String light = String.valueOf(getAdcValue(p, 4));
        String batteryVolt = MathTools.divideBy100(calcVoltage(getAdcValue(p, 6)));
        String counter = String.valueOf(p.getMessage()[8]);

        logger.info("Relative humidity " + humidity);
        logger.info("Temperature " + temp);
        logger.info("Light " + light);
        logger.info("Counter " + counter);
        logger.info("Battery " + batteryVolt);

        // record received data in db
        influxDBConnector.recordSensorData(
                sensorRepo.get(p.getRemoteAddr()),
                Optional.of(temp),
                Optional.of(humidity),
                Optional.of(counter),
                Optional.of(light),
                Optional.of(batteryVolt)
        );

        return true;
    }

    private static int getAdcValue(SerialConnection.RfPacket packet, Integer pos) {
        int valueHigh = packet.getMessage()[pos];
        int valueLow = packet.getMessage()[pos + 1];
        return ByteTools.merge(valueLow, valueHigh);
    }

    private int calcVoltage(int adcValue) {
        return ((102300 / adcValue) * 6) / 10;
    }

    public static int getTemperature(SerialConnection.RfPacket packet) {
        return (int) ((((((float) ByteTools.merge(packet.getMessage()[3], packet.getMessage()[2])) / 16384F) * 165) - 40) * 100);
    }

    public static int getRelativeHumidity(SerialConnection.RfPacket packet) {
        return (int) ((((float) ByteTools.merge(packet.getMessage()[1], packet.getMessage()[0])) / 16384F) * 100 * 100);
    }
}
