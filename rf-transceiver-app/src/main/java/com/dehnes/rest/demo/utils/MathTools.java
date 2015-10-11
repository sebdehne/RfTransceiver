package com.dehnes.rest.demo.utils;

import java.math.BigDecimal;

public class MathTools {

    public static void main(String[] args) {
        System.out.println(divideBy100(2021));
    }

    public static String divideBy100(Integer in) {
        return String.valueOf(new BigDecimal(in).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP).floatValue());
    }

}
