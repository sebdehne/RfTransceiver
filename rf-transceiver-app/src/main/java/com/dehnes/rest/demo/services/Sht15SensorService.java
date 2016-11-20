package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.utils.ByteTools;

public class Sht15SensorService {

    public static int getTemperature(SerialConnection.RfPacket p) {
        return calcTemp(ByteTools.merge(p.getMessage()[1], p.getMessage()[0]));
    }

    public static int calcTemp(int in) {
        return (int) ((((float) in * 0.01) + -40.1F) * 100);
    }

    public static Integer getRelativeHumidity(SerialConnection.RfPacket rfPacket, int temperature) {
        return calcHum(ByteTools.merge(rfPacket.getMessage()[3], rfPacket.getMessage()[2]), temperature);
    }

    public static Integer calcHum(int SO_rh, int temperature) {
        double temp = ((float) temperature) / 100F;
        double c1 = -2.0468F;
        double c2 = 0.0367F;
        double c3 = (-1.5955) / 1000000;

        // calc rhLin
        double rhLin = c1 + (c2 * SO_rh) + (c3 * Math.pow(SO_rh, 2));

        double t1 = 0.01;
        double t2 = 0.00008;
        double rHtrue = (temp - 25) * (t1 + (t2 * SO_rh)) + rhLin;

        return (int) (rHtrue * 100F);
    }

    public static void main(String[] args) {
        int temp = calcTemp(ByteTools.merge(120, 24));
        System.out.println(temp);
        System.out.println(calcHum(ByteTools.merge(160, 4), temp));
    }
}
