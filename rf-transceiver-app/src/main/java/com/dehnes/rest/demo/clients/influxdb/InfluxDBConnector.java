package com.dehnes.rest.demo.clients.influxdb;

import com.dehnes.rest.demo.services.SensorRepo;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InfluxDBConnector {
    private final static Logger logger = LoggerFactory.getLogger(InfluxDBConnector.class);

    private static final String dbName = "sensor_data";
    private static final String baseUrl = "http://localhost:8086";


    public InfluxDBConnector() {
        createDb();
    }

    private void createDb() {
        String createDbQuery = "CREATE DATABASE " + dbName;
        String retentionPolicy = "alter RETENTION POLICY default ON sensor_data DURATION 260w"; // 5 years
        //
        try {
            Request.Get("http://localhost:8086/query?q=" + URLEncoder.encode(createDbQuery, "UTF-8")).execute();
            Request.Get("http://localhost:8086/query?q=" + URLEncoder.encode(retentionPolicy, "UTF-8")).execute();
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
            Optional<Integer> light,
            Optional<Integer> batVolt) {


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
        if (batVolt.isPresent()) {
            values.add(new KeyValue("battery_volt", String.valueOf(batVolt.get())));
        }

        if (values.size() > 0) {
            recordSensorData("sensor", Optional.of(new KeyValue("room", sensorDef.getName())), values);
        }
    }

    public void recordSensorData(String type, Optional<KeyValue> tag, List<KeyValue> values) {
        try {

            StringBuilder sb = new StringBuilder();
            sb.append(type);

            if (tag.isPresent()) {
                sb.append(",").append(tag.get().key).append("=").append(tag.get().value);
            }
            sb.append(" ").append(values.stream().map(v -> v.key + "=" + v.value).collect(Collectors.joining(",")));

            logger.debug("About to send {}", sb);

            Response result = Request.Post(baseUrl + "/write?db=" + dbName)
                    .bodyString(sb.toString(), ContentType.TEXT_PLAIN)
                    .execute();

            HttpResponse httpResponse = result.returnResponse();
            if (httpResponse != null && httpResponse.getStatusLine() != null && httpResponse.getStatusLine().getStatusCode() > 299) {
                throw new RuntimeException("Could not write to InFluxDb" + httpResponse);
            }

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
        query += " order by time desc LIMIT " + limit;

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
