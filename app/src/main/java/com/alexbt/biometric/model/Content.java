package com.alexbt.biometric.model;

import java.util.Map;

public class Content{
    private String id;
    private String form_id;
    private String status;
    private Map<String, Map<String, Object>> answers;

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Map<String, Object>> getAnswers() {
        return answers;
    }
}
