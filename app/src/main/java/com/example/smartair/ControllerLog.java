package com.example.smartair;

public class ControllerLog {
    public long timestamp;
    public int doses;

    public ControllerLog() {}

    public ControllerLog(long timestamp, int doses) {
        this.timestamp = timestamp;
        this.doses = doses;
    }
}
