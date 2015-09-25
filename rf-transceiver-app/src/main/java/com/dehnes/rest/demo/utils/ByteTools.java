package com.dehnes.rest.demo.utils;

public class ByteTools {

    public static int merge(int low, int hi) {
        hi = hi << 8;
        return hi + low;
    }

}
