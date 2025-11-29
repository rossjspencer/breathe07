package com.example.smartair.R4.model;

public class PefEntry {

    private String id;
    private String childId;
    private long timestampMillis;
    private int pefValue;
    private boolean preMedication;  // true = pre, false = post

    public PefEntry() { }

    public PefEntry(String id,
                    String childId,
                    long timestampMillis,
                    int pefValue,
                    boolean preMedication) {
        this.id = id;
        this.childId = childId;
        this.timestampMillis = timestampMillis;
        this.pefValue = pefValue;
        this.preMedication = preMedication;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public int getPefValue() { return pefValue; }
    public void setPefValue(int pefValue) { this.pefValue = pefValue; }

    public boolean isPreMedication() { return preMedication; }
    public void setPreMedication(boolean preMedication) { this.preMedication = preMedication; }
}
