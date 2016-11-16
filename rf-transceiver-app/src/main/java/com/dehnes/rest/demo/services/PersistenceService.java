package com.dehnes.rest.demo.services;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PersistenceService {
    private final String filename;
    private final Properties properties;

    public PersistenceService() {
        this.properties = new Properties();
        this.filename = System.getProperty("STORAGE_FILE_NAME", "storage.properties");
        this.load();
    }

    public synchronized String get(String key, String persistDefaultValue) {
        String value = properties.getProperty(key);
        if (value == null && persistDefaultValue != null) {
            value = persistDefaultValue;
            set(key, persistDefaultValue);
        }
        return value;
    }

    public synchronized void set(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
        write();
    }

    private void load() {
        try (FileInputStream fis = new FileInputStream(filename)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write() {
        try (FileOutputStream fos = new FileOutputStream(filename, false)) {
            properties.store(fos, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
