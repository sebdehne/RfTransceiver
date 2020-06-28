package com.dehnes.rest.demo.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathToolsTest {

    @Test
    public void test() {
        assertEquals("23.23", MathTools.divideBy100(2323));
    }

}