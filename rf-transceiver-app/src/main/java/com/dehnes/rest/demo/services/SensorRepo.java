package com.dehnes.rest.demo.services;

import com.dehnes.rest.demo.services.temperature.TemperatureConfig;

import java.util.*;

public class SensorRepo {

    public enum VersionDef {
        HW_REV_4_V1(Optional.of(0), Optional.of(2), Optional.of(4), Optional.<Integer>empty(), Optional.<Integer>empty()),
        HW_REV_5_V1(Optional.of(0), Optional.of(2), Optional.of(4), Optional.of(5), Optional.<Integer>empty()),
        HW_REV_5_V2(Optional.of(0), Optional.of(2), Optional.of(4), Optional.of(5), Optional.of(7));

        private final Optional<Integer> temperaturePos;
        private final Optional<Integer> humidityPos;
        private final Optional<Integer> counterPos;
        private final Optional<Integer> lightPos;
        private final Optional<Integer> batteryVoltPos;

        VersionDef(
                Optional<Integer> temperaturePos,
                Optional<Integer> humidityPos,
                Optional<Integer> counterPos,
                Optional<Integer> lightPos,
                Optional<Integer> batteryVoltPos) {
            this.temperaturePos = temperaturePos;
            this.humidityPos = humidityPos;
            this.counterPos = counterPos;
            this.lightPos = lightPos;
            this.batteryVoltPos = batteryVoltPos;
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

        public Optional<Integer> getBatteryVoltPos() {
            return batteryVoltPos;
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
                new SensorDef(2, VersionDef.HW_REV_4_V1, "bath", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(3, VersionDef.HW_REV_4_V1, "storage", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(4, VersionDef.HW_REV_4_V1, "out_rear", TemperatureConfig.ThermistorConfig.T_4K7),
                new SensorDef(5, VersionDef.HW_REV_4_V1, "out_front", TemperatureConfig.ThermistorConfig.T_4K7),
                new SensorDef(7, VersionDef.HW_REV_5_V1, "tv_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(8, VersionDef.HW_REV_5_V2, "living_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(9, VersionDef.HW_REV_5_V1, "hallway_down", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(10, VersionDef.HW_REV_5_V1, "sleeping_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(11, VersionDef.HW_REV_5_V1, "kitchen", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(12, VersionDef.HW_REV_5_V1, "mynthe_room", TemperatureConfig.ThermistorConfig.T_10K),
                new SensorDef(13, VersionDef.HW_REV_5_V1, "noan_room", TemperatureConfig.ThermistorConfig.T_10K)
        ).forEach(s -> put(s.id, s));
    }});

    public SensorDef getSensorDef(int senderId) {
        return sensorConfig.get(senderId);
    }

}
