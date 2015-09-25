package com.dehnes.rest.demo.clients.influxdb;

import com.dehnes.rest.demo.services.SensorRepo;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

public class InfluxDBConnector {

    private static final String dbName = "sensor_data";

    public InfluxDBConnector() {
        try {
            Request.Get("http://localhost:8086/query?q=" + URLEncoder.encode("CREATE DATABASE " + dbName, "UTF-8")).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addField(StringBuilder fields, String name, Optional<Integer> value) {
        if (value.isPresent()) {
            if (fields.length() > 0) {
                fields.append(",");
            }
            fields.append(name).append("=").append(value.get());
        }
    }

    public void recordSensorData(
            SensorRepo.SensorDef sensorDef,
            Optional<Integer> temp,
            Optional<Integer> humidity,
            Optional<Integer> counter,
            Optional<Integer> light) {


        StringBuilder fields = new StringBuilder();
        addField(fields, "temperature", temp);
        addField(fields, "humidity", humidity);
        addField(fields, "counter", counter);
        addField(fields, "light", light);

        if (fields.length() > 0) {
            try {
                String body = "sensor,room=" + sensorDef.getName() + " " + fields.toString();
                Request.Post("http://localhost:8086/write?db=" + dbName)
                        .bodyString(body, ContentType.TEXT_PLAIN)
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
