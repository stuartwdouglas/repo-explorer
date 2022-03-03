package com.github.stuartwouglas;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class SimpleTest {

    @Test
    public void doStuff() throws UnsupportedEncodingException {
        String data = "\u2019]";
        System.out.println(new String("\u2019".getBytes(StandardCharsets.UTF_8)));
        System.out.println(new String("\u2019]".getBytes(StandardCharsets.UTF_8), "GB18030"));
    }

}
