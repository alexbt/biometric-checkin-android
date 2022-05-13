package com.alexbt.biometric.model;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class CheckinYear  implements Serializable {
    private String year;
    private Map<String, CheckinMonth> months = new TreeMap<>();

    public void setYear(String year) {
        this.year = year;
    }

    public Map<String, CheckinMonth> getMonths() {
        return months;
    }
}
