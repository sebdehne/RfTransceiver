package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.utils.ByteTools;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class GarageDoorService {
    private static final Logger logger = LoggerFactory.getLogger(GarageDoorService.class);
    private static final int sender_id = 24;

    private final String dbType = "garage";
    private final SerialConnection serialConnection;
    private final Function<SerialConnection.RfPacket, Boolean> listener;
    private final InfluxDBConnector influxDBConnector;
    private final CommandSender commandSender;

    public GarageDoorService(SerialConnection serialConnection, InfluxDBConnector influxDBConnector, CommandSender commandSender) {
        this.serialConnection = serialConnection;
        this.influxDBConnector = influxDBConnector;
        this.commandSender = commandSender;
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

    private boolean handleIncoming(SerialConnection.RfPacket rfPacket) {
        if (rfPacket.getRemoteAddr() != sender_id) {
            return false;
        }

        /*
         * ch1 light
         * 0  : ON
         * >0 : OFF
         *
         * ch2 broken
         *
         * ch3
         * >100 : door is not closed
         * <100 : door is closed
         */
        int[] msg = rfPacket.getMessage();
        int ch1 = ByteTools.merge(msg[0], msg[1]);
        //int ch2 = ByteTools.merge(msg[2], msg[3]);
        int ch3 = ByteTools.merge(msg[4], msg[5]);

        boolean lightIsOn = true;
        if (ch1 > 10) {
            lightIsOn = false;
        }

        boolean doorIsOpen = false;
        if (ch3 > 100) {
            doorIsOpen = true;
        }

        logger.info("Garage door. Light=" + lightIsOn + ", Door=" + doorIsOpen);

        influxDBConnector.recordSensorData(dbType, Optional.<InfluxDBConnector.KeyValue>empty(), Arrays.asList(
                new InfluxDBConnector.KeyValue("light", String.valueOf(lightIsOn)),
                new InfluxDBConnector.KeyValue("door", String.valueOf(doorIsOpen))
        ));

        return true;
    }

    public boolean sendOpenCommand() {
        return commandSender.sendValue(sender_id, 1, true);
    }

    public boolean sendCloseCommand() {
        return commandSender.sendValue(sender_id, 2, true);
    }

    public Optional<StatusRecord> getCurrentState() {
        long min = System.currentTimeMillis() - (3600 * 1000); // 60min back in time
        JSONObject json = influxDBConnector.queryRaw(dbType, Optional.<InfluxDBConnector.KeyValue>empty(), Optional.of(min), Optional.<Long>empty(), 1);
        JSONArray results = json.optJSONArray("results");
        if (results != null && results.length() > 0) {
            JSONObject result = results.getJSONObject(0);
            JSONArray series = result.optJSONArray("series");
            if (series != null && series.length() > 0) {
                JSONObject serie = series.getJSONObject(0);
                JSONArray values = serie.optJSONArray("values");
                if (values != null && values.length() > 0) {
                    JSONArray entry = values.getJSONArray(values.length() - 1);
                    boolean door = entry.getBoolean(1);
                    boolean light = entry.getBoolean(2);
                    return Optional.of(new StatusRecord(light, door));
                }
            }
        }
        return Optional.empty();
    }

    public static class StatusRecord {
        private final boolean lightIsOn;
        private final boolean doorIsOpen;

        public StatusRecord(boolean lightIsOn, boolean doorIsOpen) {
            this.lightIsOn = lightIsOn;
            this.doorIsOpen = doorIsOpen;
        }

        public boolean isDoorIsOpen() {
            return doorIsOpen;
        }

        public boolean isLightIsOn() {
            return lightIsOn;
        }
    }


}
