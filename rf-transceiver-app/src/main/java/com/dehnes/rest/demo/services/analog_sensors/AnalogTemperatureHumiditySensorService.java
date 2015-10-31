package com.dehnes.rest.demo.services.analog_sensors;

import com.dehnes.rest.demo.clients.influxdb.InfluxDBConnector;
import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.services.analog_sensors.humidity.HumidityService;
import com.dehnes.rest.demo.utils.ByteTools;
import com.dehnes.rest.demo.utils.MathTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.function.Function;

public class AnalogTemperatureHumiditySensorService {

    private static final Logger logger = LoggerFactory.getLogger(AnalogTemperatureHumiditySensorService.class);

    private final SensorRepo sensorRepo;
    private final SerialConnection serialConnection;
    private final Function<SerialConnection.RfPacket, Boolean> listener;
    private final HumidityService humidityService;
    private final InfluxDBConnector influxDBConnector;

    public AnalogTemperatureHumiditySensorService(SerialConnection serialConnection, SensorRepo sensorRepo, HumidityService humidityService, InfluxDBConnector influxDBConnector) {
        this.serialConnection = serialConnection;
        this.sensorRepo = sensorRepo;
        this.humidityService = humidityService;
        this.influxDBConnector = influxDBConnector;
        listener = this::handleIncoming;
    }

    @PostConstruct
    public void start() {
        serialConnection.registerListener(listener);
    }

    @PreDestroy
    public void stop() {
        serialConnection.unregisterListener(listener);
    }

    private boolean handleIncoming(SerialConnection.RfPacket packet) {
        int sensorId = packet.getRemoteAddr();
        SensorRepo.SensorDef sensorDef = sensorRepo.getSensorDef(sensorId);

        if (sensorDef == null) {
            return false;
        }

        Optional<String> temp = Optional.empty();
        Optional<String> humidity = Optional.empty();
        Optional<String> counter = Optional.empty();
        Optional<String> light = Optional.empty();
        Optional<String> batVolt = Optional.empty();

        if (sensorDef.getVersion().getTemperaturePos().isPresent()) {
            Optional<Integer> temperature = sensorDef.getThermistorConfig().tempValueToResistence(
                    getAdcValue(packet, sensorDef.getVersion().getTemperaturePos().get()));

            if (temperature.isPresent()) {
                temp = temperature.map(MathTools::divideBy100);
                logger.info(sensorDef.getName() + " - temperature: " + temp.get());

                if (sensorDef.getVersion().getHumidityPos().isPresent()) {
                    humidity = humidityService.extractHumdity(sensorDef.getVersion().getHumidityPos().get(), packet, temperature.get());
                    if (humidity.isPresent()) {
                        logger.info(sensorDef.getName() + " - relative humidity: " + humidity.get());
                    } else {
                        logger.warn("Failed to calc humidity");
                    }
                }
            } else {
                logger.warn("Failed to calc temp");
            }
        }
        if (sensorDef.getVersion().getCounterPos().isPresent()) {
            counter = Optional.of(String.valueOf(packet.getMessage()[sensorDef.getVersion().getCounterPos().get()]));
        }
        if (sensorDef.getVersion().getLightPos().isPresent()) {
            light = Optional.of(String.valueOf(getAdcValue(packet, sensorDef.getVersion().getLightPos().get())));
        }

        if (sensorDef.getVersion().getBatteryVoltPos().isPresent()) {
            batVolt = Optional.of(MathTools.divideBy100(calcVoltage(getAdcValue(packet, sensorDef.getVersion().getBatteryVoltPos().get()))));
        }

        // record received data in db
        influxDBConnector.recordSensorData(sensorDef, temp, humidity, counter, light, batVolt);

        return true;
    }

    private static int getAdcValue(SerialConnection.RfPacket packet, Integer pos) {
        int valueLow = packet.getMessage()[pos];
        int valueHigh = packet.getMessage()[pos + 1];
        return ByteTools.merge(valueLow, valueHigh);
    }

    private int calcVoltage(int adcValue) {
        return ((102300 / adcValue) * 6 ) / 10;
    }


}
