package com.example.smartair.r3;

public abstract class MedicineLogEntry {
    public String name;
    public int doseCount;
    public String timestamp;
    public String id;

    // firebase constructor
    public MedicineLogEntry(){};

    public MedicineLogEntry(String name, int doseCount, String timestamp) {
        this.name = name;
        this.doseCount = doseCount;
        this.timestamp = timestamp;
    }
}
