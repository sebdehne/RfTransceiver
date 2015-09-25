package com.dehnes.rest.demo.utils;

public class ADCTools {

    // PIC16F90 has a 10 bit ADC
    private static final int maxAdc = 1024;

    public static int resistance(int voltageDividerResistance, int adcValue) {
        return ((maxAdc * voltageDividerResistance) / adcValue) - voltageDividerResistance;
    }

}
