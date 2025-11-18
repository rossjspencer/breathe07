package com.b07.asthmaid.r3;

public class RescueLogEntry extends MedicineLogEntry {

    public RescueLogEntry(int doseCount, String timestamp) {
        super(doseCount, timestamp);
    }

    @Override
    public void storeEntry(MedicineLog log) {
        log.addEntry(this);
    }
}
