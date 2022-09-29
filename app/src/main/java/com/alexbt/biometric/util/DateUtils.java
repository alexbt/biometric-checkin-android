package com.alexbt.biometric.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    public static String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("gmt"));
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA_FRENCH);
        return DATE_FORMAT.format(c.getTime());
    }

    public static String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA_FRENCH);
        return DATE_FORMAT.format(c.getTime());
    }
}
