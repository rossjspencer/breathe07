package com.example.smartair.R4.model;

public class ZoneChangeEntry {

    private String id;
    private String childId;
    private long timestampMillis;
    private String previousZone;
    private String newZone;
    private int pefValueAtChange;

    public ZoneChangeEntry() { }

    public ZoneChangeEntry(String id,
                           String childId,
                           long timestampMillis,
                           String previousZone,
                           String newZone,
                           int pefValueAtChange) {
        this.id = id;
        this.childId = childId;
        this.timestampMillis = timestampMillis;
        this.previousZone = previousZone;
        this.newZone = newZone;
        this.pefValueAtChange = pefValueAtChange;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public String getPreviousZone() { return previousZone; }
    public void setPreviousZone(String previousZone) { this.previousZone = previousZone; }

    public String getNewZone() { return newZone; }
    public void setNewZone(String newZone) { this.newZone = newZone; }

    public int getPefValueAtChange() { return pefValueAtChange; }
    public void setPefValueAtChange(int pefValueAtChange) { this.pefValueAtChange = pefValueAtChange; }
}
