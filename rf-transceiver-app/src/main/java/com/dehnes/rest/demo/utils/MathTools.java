package com.dehnes.rest.demo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathTools {

    public static void main(String[] args) {
        System.out.println(divideBy100(2021));
    }

    public static String divideBy100(Integer in) {
        return String.valueOf(
                new BigDecimal(in)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        ).floatValue()
        );
    }

}
