package com.example.template.demo.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Disabled
public class PingItTest {

    private static String url = "http://"
            + System.getProperty("PING_APP_HOSTNAME", "localhost") + ":"
            + System.getProperty("PING_APP_PORT", "9090");

    private static final RestClient restClient = new RestClient(url);

    @Test
    public void testPing() throws IOException {
        restClient.executeGet("/ping", "$.response", (code, response) -> {
            Assertions.assertEquals(200, code.intValue());
            Assertions.assertEquals("PONG", response);
        });
    }

    @Test
    public void testNotFound() throws IOException {
        restClient.executeGet("/not_there", "$.response", (code, response) -> {
            Assertions.assertEquals(404, code.intValue());
            Assertions.assertTrue(response.startsWith("No handler found for "));
        });
    }
}
