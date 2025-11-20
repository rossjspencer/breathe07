package com.b07.asthmaid.r3;

public abstract class MedicineLogEntry {
    public int doseCount;
    public String timestamp;
    public String id;

    // firebase constructor
    public MedicineLogEntry(){};

    public MedicineLogEntry(int doseCount, String timestamp) {
        this.doseCount = doseCount;
        this.timestamp = timestamp;
    }

    public abstract void storeEntry(MedicineLog log);
}
