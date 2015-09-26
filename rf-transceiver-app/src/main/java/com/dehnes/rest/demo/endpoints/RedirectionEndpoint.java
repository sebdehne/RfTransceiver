package com.dehnes.rest.demo.endpoints;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dehnes.rest.demo.Main;
import com.dehnes.rest.server.config.TriConsumer;

public class RedirectionEndpoint implements TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> {

    @Override
    public void accept(HttpServletRequest request, HttpServletResponse response, List<String> strings) {
        String host = request.getHeader("Host");
        if (host == null || host.length() == 0) {
            host = "localhost";
            int port = Main.PORT;
            if (port != 80) {
                host += ":" + port;
            }
        }

        String baseUrl = "http://" + host;
        response.setHeader("Location", System.getProperty("EXTERNAL_URL", baseUrl) + "/index.html");
        response.setStatus(301);

        // write "something" such that the response becomes committed
        try {
            response.getOutputStream().write(new byte[0]);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
