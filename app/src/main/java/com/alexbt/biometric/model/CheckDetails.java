package com.alexbt.biometric.model;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class CheckDetails implements Serializable {
    private Map<String, CheckinYear> checkinYears = new TreeMap<>();
    private String lastCheckinDate;

    public void addCheckin(String year, String month, String day){
        String currentCheckin = String.format("%s-%s-%s", year, month, day);
        CheckinYear checkinYear = checkinYears.get(year);
        if(checkinYear==null){
            checkinYear = new CheckinYear();
            checkinYear.setYear(year);
            checkinYears.put(year, checkinYear);
        }
        CheckinMonth checkinMonth = checkinYear.getMonths().get(month);
        if(checkinMonth==null){
            checkinMonth = new CheckinMonth();
            checkinYear.getMonths().put(month, checkinMonth);
            checkinMonth.setMonth(month);
        }
        checkinMonth.addCheckin(currentCheckin);

        if (this.lastCheckinDate == null || currentCheckin.compareTo(this.lastCheckinDate) > 1) {
            this.lastCheckinDate = currentCheckin;
        }
    }

    public String getLastCheckinDate() {
        return lastCheckinDate;
    }

    public Map<String, CheckinYear> getCheckinYears() {
        return checkinYears;
    }
}
