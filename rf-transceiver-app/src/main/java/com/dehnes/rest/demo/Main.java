package com.dehnes.rest.demo;

import com.dehnes.rest.demo.services.analog_sensors.AnalogTemperatureHumiditySensorService;
import com.dehnes.rest.demo.services.chipcap2_sensors.ChipCap2SensorService;
import com.dehnes.rest.server.EmbeddedJetty;
import com.dehnes.rest.server.config.AppContext;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static final int PORT = Integer.parseInt(System.getProperty("JETTY_PORT", "9090"));

    public static void main(String[] args) throws Exception {
        System.setProperty("DST_HOST", "192.168.1.1");

        AppContext config = new AppContext();
        config.addInstance(ExecutorService.class, Executors.newCachedThreadPool());

        Server server = new EmbeddedJetty().start(
                PORT,
                config.getInstance(Routes.class)
        );
        config.getInstance(AnalogTemperatureHumiditySensorService.class);
        config.getInstance(ChipCap2SensorService.class);
        config.start();

        server.join();
    }
}
