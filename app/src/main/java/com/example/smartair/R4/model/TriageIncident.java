package com.example.smartair.R4.model;

public class TriageIncident {
    public String id, childId;
    public long timestampMillis;
    public boolean redCannotSpeak, redRetractions, redBlueLips;
    public Integer recentRescueAttempts;
    public Integer pefAtIncident;
    public String decision;     // EMERGENCY / HOME_STEPS / MONITOR
    public String userResponse;
    public boolean escalated;

    public TriageIncident() {}
}
