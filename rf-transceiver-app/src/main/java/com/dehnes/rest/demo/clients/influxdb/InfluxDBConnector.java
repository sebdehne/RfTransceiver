package com.dehnes.rest.demo.clients.influxdb;

import com.dehnes.rest.demo.services.SensorRepo;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InfluxDBConnector {

    private static final String dbName = "sensor_data";
    private static final String baseUrl = "http://localhost:8086";


    public InfluxDBConnector() {
        try {
            Request.Get("http://localhost:8086/query?q=" + URLEncoder.encode("CREATE DATABASE " + dbName, "UTF-8")).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class KeyValue {
        private final String key;
        private final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public void recordSensorData(
            SensorRepo.SensorDef sensorDef,
            Optional<Integer> temp,
            Optional<Integer> humidity,
            Optional<Integer> counter,
            Optional<Integer> light) {


        List<KeyValue> values = new LinkedList<>();
        if (temp.isPresent()) {
            values.add(new KeyValue("temperature", String.valueOf(temp.get())));
        }
        if (humidity.isPresent()) {
            values.add(new KeyValue("humidity", String.valueOf(humidity.get())));
        }
        if (counter.isPresent()) {
            values.add(new KeyValue("counter", String.valueOf(counter.get())));
        }
        if (light.isPresent()) {
            values.add(new KeyValue("light", String.valueOf(light.get())));
        }

        if (values.size() > 0) {
            recordSensorData("sensor", Optional.of(new KeyValue("room", sensorDef.getName())), values);
        }
    }

    public void recordSensorData(String type, Optional<KeyValue> tag, List<KeyValue> values) {
        try {

            StringBuilder sb = new StringBuilder();
            sb.append(type).append(" ");

            if (tag.isPresent()) {
                sb.append(",").append(tag.get().key).append("=").append(tag.get().value).append(" ");
            }
            sb.append(values.stream().map(v -> v.key + "=" + v.value).collect(Collectors.joining(",")));

            Request.Post(baseUrl + "/write?db=" + dbName)
                    .bodyString(sb.toString(), ContentType.TEXT_PLAIN)
                    .execute();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public JSONObject queryRaw(String type, Optional<KeyValue> targetTag, Optional<Long> timeMin, Optional<Long> timeMax, int limit) {
        String query = "SELECT * FROM " + type + " WHERE 1 = 1";
        if (targetTag.isPresent()) {
            query += " AND " + targetTag.get().key + "=" + targetTag.get().value;
        }
        if (timeMin.isPresent()) {
            query += " AND time > " + (timeMin.get() / 1000) + "s";
        }
        if (timeMax.isPresent()) {
            query += " AND time < " + (timeMax.get() / 1000) + "s";
        }
        query += " LIMIT " + limit;

        try {
            URIBuilder b = new URIBuilder(baseUrl + "/query");
            b.addParameter("db", dbName);
            b.addParameter("q", query);

            Response execute = Request.Get(b.build()).execute();
            return new JSONObject(execute.returnContent().asString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
