package com.example.smartair;

import java.util.List;

public class SymptomLog {
    public long timestamp;
    public List<String> symptoms;
    public List<String> triggers;
    public int severity;
    public String activityLimitation;
    public String notes;
    public String loggedBy;

    public SymptomLog() {}

    public SymptomLog(long timestamp, List<String> symptoms, List<String> triggers, int severity, String activityLimitation, String notes, String loggedBy) {
        this.timestamp = timestamp;
        this.symptoms = symptoms;
        this.triggers = triggers;
        this.severity = severity;
        this.activityLimitation = activityLimitation;
        this.notes = notes;
        this.loggedBy = loggedBy;
    }
}