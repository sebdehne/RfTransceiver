package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.clients.serial.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CommandSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandSender.class);

    private final SerialConnection serialConnection;

    public CommandSender(SerialConnection serialConnection) {
        this.serialConnection = serialConnection;
    }

    public boolean sendValue(int rfAddr, int value) {
        boolean result = false;
        try {
            result = sendValueNow(rfAddr, value);
            Thread.sleep(100);
            result = sendValueNow(rfAddr, value);
            Thread.sleep(100);
            result = sendValueNow(rfAddr, value);
            Thread.sleep(100);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return result;
    }

    private boolean sendValueNow(int rfAddr, int value) {
        return serialConnection.send(new SerialConnection.RfPacket(
                rfAddr, new int[]{value}
        ), 5, TimeUnit.SECONDS);
    }


}
