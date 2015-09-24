package com.dehnes.rest.demo.client;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialConnectionTest {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        SerialConnection serialConnection = new SerialConnection(
                executorService,
                new InetSocketAddress("home.dehnes.com", 23000)
        );
        serialConnection.start();
        Thread.sleep(1000 * 60 * 30);
        serialConnection.stop();
    }

}
