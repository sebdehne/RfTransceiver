package com.dehnes.rest.demo.clients.serial;

public class SerialConnectionTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("DST_HOST", "home.dehnes.com");
        SerialConnection serialConnection = new SerialConnection();
        serialConnection.start();
        Thread.sleep(1000 * 60 * 30);
        serialConnection.stop();
    }

}
