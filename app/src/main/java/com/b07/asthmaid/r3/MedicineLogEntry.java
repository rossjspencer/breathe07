package com.b07.asthmaid.r3;

public abstract class MedicineLogEntry {
    int doseCount;
    String timestamp;

    public MedicineLogEntry(int doseCount, String timestamp) {
        this.doseCount = doseCount;
        this.timestamp = timestamp;
    }

    public abstract void storeEntry(MedicineLog log);
}
