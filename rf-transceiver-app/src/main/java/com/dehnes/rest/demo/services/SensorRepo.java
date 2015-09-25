package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.services.temperature.TemperatureConfig;

import java.util.*;

public class SensorRepo {

    public enum VersionDef {
        VERSION_4(Optional.of(0), Optional.of(2), Optional.of(4), Optional.<Integer>empty()),
        VERSION_5(Optional.of(0), Optional.of(2), Optional.of(4), Optional.of(5));

        private final Optional<Integer> temperaturePos;
        private final Optional<Integer> humidityPos;
        private final Optional<Integer> counterPos;
        private final Optional<Integer> lightPos;

        VersionDef(Optional<Integer> temperaturePos, Optional<Integer> humidityPos, Optional<Integer> counterPos, Optional<Integer> lightPos) {
            this.temperaturePos = temperaturePos;
            this.humidityPos = humidityPos;
            this.counterPos = counterPos;
            this.lightPos = lightPos;
        }

        public Optional<Integer> getTemperaturePos() {
            return temperaturePos;
        }

        public Optional<Integer> getHumidityPos() {
            return humidityPos;
        }

        public Optional<Integer> getCounterPos() {
            return counterPos;
        }

        public Optional<Integer> getLightPos() {
            return lightPos;
        }
    }


    public class SensorDef {
        private final int id;
        private final VersionDef version;
        private final String name;
        private final TemperatureConfig.ThermistorConfig thermistorConfig;

        public SensorDef(int id, VersionDef version, String name, TemperatureConfig.ThermistorConfig thermistorConfig) {
            this.id = id;
            this.version = version;
            this.name = name;
            this.thermistorConfig = thermistorConfig;
        }

        public int getId() {
            return id;
        }

        public VersionDef getVersion() {
            return version;
        }

        public String getName() {
            return name;
        }

        public TemperatureConfig.ThermistorConfig getThermistorConfig() {
            return thermistorConfig;
        }
    }

    private final Map<Integer, SensorDef> sensorConfig = Collections.unmodifiableMap(new HashMap<Integer, SensorDef>() {{
        Arrays.asList(
                new SensorDef(2, VersionDef.VERSION_4, "bath", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(3, VersionDef.VERSION_4, "storage", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(4, VersionDef.VERSION_4, "out_rear", TemperatureConfig.ThermistorConfig.T_4K7),
                new SensorDef(5, VersionDef.VERSION_4, "out_front", TemperatureConfig.ThermistorConfig.T_4K7),
                new SensorDef(7, VersionDef.VERSION_5, "tv_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(8, VersionDef.VERSION_5, "living_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(9, VersionDef.VERSION_5, "hallway_down", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(10, VersionDef.VERSION_5, "sleeping_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(11, VersionDef.VERSION_5, "kitchen", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(12, VersionDef.VERSION_5, "mynthe_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(13, VersionDef.VERSION_5, "noan_room", TemperatureConfig.ThermistorConfig.T_10K)
        ).forEach(s -> put(s.id, s));
    }});

    public SensorDef getSensorDef(int senderId) {
        return sensorConfig.get(senderId);
    }

}
