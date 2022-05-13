package com.alexbt.biometric.model;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

public class CheckinMonth  implements Serializable {
    private String month;
    private Set<String> dates = new TreeSet<>();

    public void setMonth(String month) {
        this.month = month;
    }

    public Set<String> getDates() {
        return dates;
    }

    public void addCheckin(String currentCheckin) {
        dates.add(currentCheckin);
    }
}
