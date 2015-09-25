package com.dehnes.rest.demo.services.humidity;

import com.dehnes.rest.demo.client.SerialConnection;
import com.dehnes.rest.demo.utils.ADCTools;
import com.dehnes.rest.demo.utils.ByteTools;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class HumidityService {

    private static class TableEntry {
        private final int resistance;
        private final int relativeHumidity;

        public TableEntry(int resistance, int relativeHumidity) {
            this.resistance = resistance * 1000;
            this.relativeHumidity = relativeHumidity;
        }
    }

    private static class TemperatureTable {
        private final int temperature;
        private final List<TableEntry> entries;

        private TemperatureTable(int temperature, List<TableEntry> entries) {
            this.temperature = temperature;
            this.entries = entries;
        }
    }

    // https://www1.elfa.se/data1/wwwroot/assets/datasheets/h25k5a_eng_jap_tds.pdf
    private static final List<TemperatureTable> lookUpTable = Collections.unmodifiableList(new LinkedList<TemperatureTable>() {{
        // 0C
        add(new TemperatureTable(0, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(Integer.MAX_VALUE, 20));
            add(new TableEntry(Integer.MAX_VALUE, 25));
            add(new TableEntry(12000, 30));
            add(new TableEntry(5200, 35));
            add(new TableEntry(2800, 40));
            add(new TableEntry(720, 45));
            add(new TableEntry(384, 50));
            add(new TableEntry(200, 55));
            add(new TableEntry(108, 60));
            add(new TableEntry(64, 65));
            add(new TableEntry(38, 70));
            add(new TableEntry(23, 75));
            add(new TableEntry(16, 80));
            add(new TableEntry(10, 85));
            add(new TableEntry(7, 90));
        }})));

        // 5C
        add(new TemperatureTable(5, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(Integer.MAX_VALUE, 20));
            add(new TableEntry(19800, 25));
            add(new TableEntry(9800, 30));
            add(new TableEntry(4700, 35));
            add(new TableEntry(2000, 40));
            add(new TableEntry(510, 45));
            add(new TableEntry(271, 50));
            add(new TableEntry(149, 55));
            add(new TableEntry(82, 60));
            add(new TableEntry(48, 65));
            add(new TableEntry(29, 70));
            add(new TableEntry(18, 75));
            add(new TableEntry(12, 80));
            add(new TableEntry(8, 85));
            add(new TableEntry(5, 90));
        }})));

        // 10C
        add(new TemperatureTable(10, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(Integer.MAX_VALUE, 20));
            add(new TableEntry(16000, 25));
            add(new TableEntry(7200, 30));
            add(new TableEntry(3200, 35));
            add(new TableEntry(1400, 40));
            add(new TableEntry(386, 45));
            add(new TableEntry(211, 50));
            add(new TableEntry(118, 55));
            add(new TableEntry(64, 60));
            add(new TableEntry(38, 65));
            add(new TableEntry(24, 70));
            add(new TableEntry(15, 75));
            add(new TableEntry(10, 80));
            add(new TableEntry(7, 85));
            add(new TableEntry(5, 90));
        }})));

        // 15C
        add(new TemperatureTable(15, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(21000, 20));
            add(new TableEntry(10500, 25));
            add(new TableEntry(5100, 30));
            add(new TableEntry(2350, 35));
            add(new TableEntry(1050, 40));
            add(new TableEntry(287, 45));
            add(new TableEntry(159, 50));
            add(new TableEntry(91, 55));
            add(new TableEntry(51, 60));
            add(new TableEntry(31, 65));
            add(new TableEntry(19, 70));
            add(new TableEntry(12, 75));
            add(new TableEntry(8, 80));
            add(new TableEntry(6, 85));
            add(new TableEntry(4, 90));
        }})));

        // 20C
        add(new TemperatureTable(20, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(13500, 20));
            add(new TableEntry(6700, 25));
            add(new TableEntry(3300, 30));
            add(new TableEntry(1800, 35));
            add(new TableEntry(840, 40));
            add(new TableEntry(216, 45));
            add(new TableEntry(123, 50));
            add(new TableEntry(70, 55));
            add(new TableEntry(40, 60));
            add(new TableEntry(25, 65));
            add(new TableEntry(16, 70));
            add(new TableEntry(10, 75));
            add(new TableEntry(7, 80));
            add(new TableEntry(4, 85));
            add(new TableEntry(3, 90));
        }})));

        // 25C
        add(new TemperatureTable(25, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(9800, 20));
            add(new TableEntry(4803, 25));
            add(new TableEntry(2500, 30));
            add(new TableEntry(1300, 35));
            add(new TableEntry(630, 40));
            add(new TableEntry(166, 45));
            add(new TableEntry(95, 50));
            add(new TableEntry(55, 55));
            add(new TableEntry(31, 60));
            add(new TableEntry(20, 65));
            add(new TableEntry(13, 70));
            add(new TableEntry(9, 75));
            add(new TableEntry(6, 80));
            add(new TableEntry(4, 85));
            add(new TableEntry(3, 90));
        }})));

        // 30C
        add(new TemperatureTable(30, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(8000, 20));
            add(new TableEntry(3900, 25));
            add(new TableEntry(2000, 30));
            add(new TableEntry(980, 35));
            add(new TableEntry(470, 40));
            add(new TableEntry(131, 45));
            add(new TableEntry(77, 50));
            add(new TableEntry(44, 55));
            add(new TableEntry(25, 60));
            add(new TableEntry(17, 65));
            add(new TableEntry(11, 70));
            add(new TableEntry(7, 75));
            add(new TableEntry(5, 80));
            add(new TableEntry(4, 85));
            add(new TableEntry(3, 90));
        }})));

        // 35C
        add(new TemperatureTable(35, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(6300, 20));
            add(new TableEntry(3100, 25));
            add(new TableEntry(1500, 30));
            add(new TableEntry(750, 35));
            add(new TableEntry(385, 40));
            add(new TableEntry(104, 45));
            add(new TableEntry(63, 50));
            add(new TableEntry(38, 55));
            add(new TableEntry(21, 60));
            add(new TableEntry(13, 65));
            add(new TableEntry(9, 70));
            add(new TableEntry(6, 75));
            add(new TableEntry(4, 80));
            add(new TableEntry(3, 85));
            add(new TableEntry(2, 90));
        }})));

        // 40C
        add(new TemperatureTable(40, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(4600, 20));
            add(new TableEntry(2300, 25));
            add(new TableEntry(1100, 30));
            add(new TableEntry(575, 35));
            add(new TableEntry(282, 40));
            add(new TableEntry(80, 45));
            add(new TableEntry(52, 50));
            add(new TableEntry(32, 55));
            add(new TableEntry(17, 60));
            add(new TableEntry(11, 65));
            add(new TableEntry(8, 70));
            add(new TableEntry(6, 75));
            add(new TableEntry(4, 80));
            add(new TableEntry(3, 85));
            add(new TableEntry(2, 90));
        }})));

        // 45C
        add(new TemperatureTable(45, Collections.unmodifiableList(new LinkedList<TableEntry>() {{
            add(new TableEntry(3800, 20));
            add(new TableEntry(1850, 25));
            add(new TableEntry(900, 30));
            add(new TableEntry(430, 35));
            add(new TableEntry(210, 40));
            add(new TableEntry(66, 45));
            add(new TableEntry(45, 50));
            add(new TableEntry(30, 55));
            add(new TableEntry(14, 60));
            add(new TableEntry(9, 65));
            add(new TableEntry(7, 70));
            add(new TableEntry(5, 75));
            add(new TableEntry(3, 80));
            add(new TableEntry(2, 85));
            add(new TableEntry(1, 90));
        }})));

    }});

    public Optional<Integer> extractHumdity(
            int humidityPos,
            SerialConnection.RfPacket packet,
            int temperature) {

        // too cold for our sensor
        if (temperature < 0) {
            return Optional.empty();
        }

        int humidityLow = packet.getMessage()[humidityPos];
        int humidityHi = packet.getMessage()[humidityPos + 1];
        int humidity = ByteTools.merge(humidityLow, humidityHi);

        int resistance = ADCTools.resistance(216000, humidity);


        // perform the lookup
        Optional<TemperatureTable> table = findTable(temperature / 100);
        if (!table.isPresent()) {
            return Optional.empty();
        }

        return findHumidity(table.get(), resistance);
    }

    private Optional<Integer> findHumidity(TemperatureTable table, int resistance) {
        TableEntry lastEntry = null;
        for (TableEntry te : table.entries) {
            if (lastEntry == null || resistance <= te.resistance) {
                lastEntry = te;
                continue;
            }

            // we found our spot, but which of the two is the closest?
            int deltaToPreviousTable = lastEntry.resistance - resistance;
            int deltaToNextTable = resistance - te.resistance;
            if (deltaToNextTable > deltaToPreviousTable) {
                return Optional.of(lastEntry.relativeHumidity);
            } else {
                return Optional.of(te.relativeHumidity);
            }
        }
        return Optional.of(100);
    }

    private Optional<TemperatureTable> findTable(int temperature) {
        if (temperature < 0 || temperature > 40) {
            return Optional.empty();
        }

        TemperatureTable lastTable = null;
        for (TemperatureTable tt : lookUpTable) {
            if (lastTable == null || temperature >= tt.temperature) {
                lastTable = tt;
                continue;
            }

            // we found our spot, but which of the two is the closest?
            int deltaToPreviousTable = temperature - lastTable.temperature;
            int deltaToNextTable = tt.temperature - temperature;
            if (deltaToNextTable > deltaToPreviousTable) {
                return Optional.of(lastTable);
            } else {
                return Optional.of(tt);
            }
        }

        // more than 45C????

        return Optional.empty();
    }

}
