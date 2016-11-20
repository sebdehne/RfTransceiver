package com.dehnes.rest.demo.clients.serial;

import com.dehnes.rest.demo.services.Sht15SensorService;
import com.dehnes.rest.demo.utils.MathTools;

import java.util.concurrent.TimeUnit;

public class SerialConnectionTest {

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("DST_HOST", "192.168.1.1");
        SerialConnection serialConnection = new SerialConnection();
        serialConnection.start();
        serialConnection.registerListener(rfPacket -> {
            System.out.println(rfPacket);
            int temperatur = Sht15SensorService.getTemperature(rfPacket);
            System.out.println("Temp: " + MathTools.divideBy100(temperatur));
            System.out.println("Humidity: " + MathTools.divideBy100(Sht15SensorService.getRelativeHumidity(rfPacket, temperatur)));
            return true;
        });
        int nextCommand = 0;
        for (int i = 0; i < 1000; i++) {
            nextCommand++;

            boolean result = serialConnection.send(new SerialConnection.RfPacket(
                    6, new int[]{nextCommand}
            ), 10, TimeUnit.SECONDS);
            System.out.println(result + " - " + nextCommand);

            Thread.sleep(1000 * 10);

            if (nextCommand >= 3) {
                nextCommand = 0;
            }
        }
        serialConnection.stop();
    }

}
