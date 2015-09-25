package com.dehnes.rest.demo.services.temperature;

import com.dehnes.rest.demo.client.SerialConnection;
import com.dehnes.rest.demo.services.SensorRepo;
import com.dehnes.rest.demo.utils.ByteTools;

import java.util.Optional;

public class TemperatureHandleService {

    public Optional<Integer> extractTemperature(int tempPos, SerialConnection.RfPacket packet, SensorRepo.SensorDef sensorDef) {
        int tempLow = packet.getMessage()[tempPos];
        int tempHi = packet.getMessage()[tempPos + 1];
        int tempValue = ByteTools.merge(tempLow, tempHi);

        return sensorDef.getThermistorConfig().tempValueToResistence(tempValue);
    }

}
