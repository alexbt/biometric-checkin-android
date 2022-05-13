package com.alexbt.biometric.util;

public class FormatUtil {

    public static String formatPhone(String no) {
        return "(" + no.substring(0, 3) + ") " + no.substring(3, 6) + "-" + no.substring(6, 10);
    }
}
