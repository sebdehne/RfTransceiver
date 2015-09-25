package com.dehnes.rest.demo.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialConnectionTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("DST_HOST", "home.dehnes.com");
        ExecutorService executorService = Executors.newCachedThreadPool();
        SerialConnection serialConnection = new SerialConnection(executorService);
        serialConnection.start();
        Thread.sleep(1000 * 60 * 30);
        serialConnection.stop();
    }

}
