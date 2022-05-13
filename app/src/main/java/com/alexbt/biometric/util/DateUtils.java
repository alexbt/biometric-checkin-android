package com.alexbt.biometric.util;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA_FRENCH);
    public static String getCurrentTime(){
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("gmt"));
        return DATE_TIME_FORMAT.format(c.getTime());
    }

    public static Calendar getCurrentTimeCalendar(){
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("gmt"));
        return c;
    }

    public static String getCurrentDate(){
        Calendar c = Calendar.getInstance();
        return DATE_FORMAT.format(c.getTime());
    }

    public static int daysBeforeToday(@NonNull String d1){
        try {
            long diff = DATE_FORMAT.parse(d1).getTime() - DATE_FORMAT.parse(getCurrentDate()).getTime();
            return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
