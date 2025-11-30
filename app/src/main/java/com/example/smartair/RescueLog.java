package com.example.smartair;

public class RescueLog {
    public long timestamp;
    public int dose;
    public boolean worseAfterDose;

    public RescueLog() {}

    public RescueLog(long timestamp, int dose, boolean worseAfterDose) {
        this.timestamp = timestamp;
        this.dose = dose;
        this.worseAfterDose = worseAfterDose;
    }
}
