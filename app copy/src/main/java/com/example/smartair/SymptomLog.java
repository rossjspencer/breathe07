package com.example.smartair;

public class SymptomLog {
    public long timestamp;
    public int severity; // simple indicator of problem day (1 = problem)

    public SymptomLog() {}

    public SymptomLog(long timestamp, int severity) {
        this.timestamp = timestamp;
        this.severity = severity;
    }
}
