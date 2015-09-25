package com.dehnes.rest.demo.services.temperature;

import com.dehnes.rest.demo.utils.ADCTools;

import java.util.Optional;

public class TemperatureConfig {

    // http://www.mouser.com/ds/2/136/LeadedDisks__B57164__K164-81893.pdf
    private static final int[][] map_10k = new int[][]{
            {-55, 1214600},
            {-50, 844390},
            {-45, 592430},
            {-40, 419380},
            {-35, 299470},
            {-30, 215670},
            {-25, 156410},
            {-20, 114660},
            {-15, 84510},
            {-10, 62927},
            {-5, 47077},
            {0, 35563},
            {5, 27119},
            {10, 20860},
            {15, 16204},
            {20, 12683},
            {25, 10000},
            {30, 7942},
            {35, 6326},
            {40, 5074},
            {45, 4102}
    };

    // http://www.mouser.com/ds/2/136/LeadedDisks__B57164__K164-81893.pdf
    private static final int[][] map_4k7 = new int[][]{
            {-55, 880520},
            {-50, 616500},
            {-45, 437270},
            {-40, 313950},
            {-35, 228020},
            {-30, 167420},
            {-25, 123670},
            {-20, 92353},
            {-15, 70079},
            {-10, 53654},
            {-5, 41260},
            {0, 32000},
            {5, 24986},
            {10, 19662},
            {15, 15596},
            {20, 12457},
            {25, 10000},
            {30, 8035},
            {35, 6534},
            {40, 5345},
            {45, 4396}
    };

    public enum ThermistorConfig {
        T_10K(10000, map_10k),
        T_4K7(8050, map_4k7);

        private final int voltageDividerResistance;
        private final int[][] correctionMap;

        ThermistorConfig(int voltageDividerResistance, int[][] correctionMap) {
            this.voltageDividerResistance = voltageDividerResistance;
            this.correctionMap = correctionMap;
        }

        public Optional<Integer> tempValueToResistence(int tempValue) {
            int resistance = ADCTools.resistance(voltageDividerResistance, tempValue);

            int lastTemp = correctionMap[0][0];
            int lastOhm = correctionMap[0][1];
            for (int[] mapEntry : correctionMap) {
                if (resistance > mapEntry[1]) {
                    int delta = (lastOhm - mapEntry[1]) / (mapEntry[0] - lastTemp);
                    int offset = lastOhm - resistance;
                    float value = lastTemp + ((float) offset / (float) delta);
                    return Optional.of((int) (value * 100));
                } else {
                    lastOhm = mapEntry[1];
                    lastTemp = mapEntry[0];
                }
            }
            return Optional.empty();
        }
    }
}
