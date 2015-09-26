package com.dehnes.rest.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageFanSpeedController {
    private final static Logger logger = LoggerFactory.getLogger(StorageFanSpeedController.class);

    private static final int senderId = 26;

    private final CommandSender commandSender;
    private volatile int currentLevel = 20; // 0 <-> 255

    public StorageFanSpeedController(CommandSender commandSender) {
        this.commandSender = commandSender;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean setNewValue(int value) {
        if (commandSender.sendValue(senderId, value)) {
            currentLevel = value;
            logger.info("Sent new value to ventilation controller in storage " + value);
            return true;
        } else {
            logger.info("Failed t send new value to ventilation controller in storage " + value);
            return false;
        }
    }
}
