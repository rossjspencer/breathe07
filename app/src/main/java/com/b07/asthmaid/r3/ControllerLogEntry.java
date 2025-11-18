package com.b07.asthmaid.r3;

public class ControllerLogEntry extends MedicineLogEntry {

    public ControllerLogEntry(int doseCount, String timestamp){
        super(doseCount, timestamp);
    }

    @Override
    public void storeEntry(MedicineLog log) {
        log.addEntry(this);
    }
}
