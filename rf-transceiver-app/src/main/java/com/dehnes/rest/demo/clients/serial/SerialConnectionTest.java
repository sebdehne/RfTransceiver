package com.dehnes.rest.demo.clients.serial;

import java.util.concurrent.TimeUnit;

public class SerialConnectionTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("DST_HOST", "192.168.1.1");
        SerialConnection serialConnection = new SerialConnection();
        serialConnection.start();
        serialConnection.registerListener(rfPacket -> {
            System.out.println(rfPacket);
            return true;
        });
        int nextCommand = 0;
        for (int i = 0; i < 1000; i++) {
            nextCommand++;

            boolean result = serialConnection.send(new SerialConnection.RfPacket(
                    27, new int[]{nextCommand}
            ), 10, TimeUnit.SECONDS);
            System.out.println(result + " - " + nextCommand);
            Thread.sleep(1000 * 5);

            if (nextCommand >= 3) {
                nextCommand = 0;
            }
        }
        serialConnection.stop();
    }

}
