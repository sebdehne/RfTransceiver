package com.dehnes.rest.demo;

import com.dehnes.rest.demo.services.temperature.TemperatureHandleService;
import org.eclipse.jetty.server.Server;

import com.dehnes.rest.demo.services.ShutdownService;
import com.dehnes.rest.server.EmbeddedJetty;
import com.dehnes.rest.server.config.AppContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        System.setProperty("DST_HOST", "home.dehnes.com");

        AppContext config = new AppContext();

        Server server = new EmbeddedJetty().start(
                Integer.parseInt(System.getProperty("JETTY_PORT", "9091")),
                config.getInstance(Routes.class)
        );

        config.addInstance(ExecutorService.class, Executors.newCachedThreadPool());
        config.getInstance(ShutdownService.class).setServer(server);
        config.getInstance(TemperatureHandleService.class);
        config.start();
        server.join();
    }
}
