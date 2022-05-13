package com.alexbt.biometric.model;

import java.util.Map;

public class Content{
    private String id;
    private String form_id;
    private String status;
    private Map<String, Map<String, Object>> answers;

    public Map<String, Map<String, Object>> getAnswers() {
        return answers;
    }
}
