package com.example.smartair.R4.model;

public class ChildProfile {

    private String childId;
    private String parentUid;
    private String name;
    private Integer personalBestPef;   // PB (nullable if not set)
    private String currentZone;        // "GREEN", "YELLOW", "RED", or null

    // Required empty constructor for Firebase
    public ChildProfile() { }

    public ChildProfile(String childId,
                        String parentUid,
                        String name,
                        Integer personalBestPef,
                        String currentZone) {
        this.childId = childId;
        this.parentUid = parentUid;
        this.name = name;
        this.personalBestPef = personalBestPef;
        this.currentZone = currentZone;
    }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getParentUid() { return parentUid; }
    public void setParentUid(String parentUid) { this.parentUid = parentUid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPersonalBestPef() { return personalBestPef; }
    public void setPersonalBestPef(Integer personalBestPef) { this.personalBestPef = personalBestPef; }

    public String getCurrentZoneRaw() { return currentZone; }
    public void setCurrentZoneRaw(String currentZone) { this.currentZone = currentZone; }

    // Convenience helpers for enums
    public com.example.smartair.R4.model.AsthmaZone getCurrentAsthmaZone() {
        if (currentZone == null) {
            return AsthmaZone.UNKNOWN;
        }
        try {
            return AsthmaZone.valueOf(currentZone);
        } catch (IllegalArgumentException e) {
            return AsthmaZone.UNKNOWN;
        }
    }

    public void setCurrentAsthmaZone(AsthmaZone zone) {
        this.currentZone = (zone == null) ? null : zone.name();
    }
}
