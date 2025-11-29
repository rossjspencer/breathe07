package com.example.smartair.R4.logic;

import com.example.smartair.R4.model.AsthmaZone;

public class TriageDecider {
    public enum Decision { EMERGENCY, HOME_STEPS, MONITOR }

    public Decision decide(boolean anyRedFlag, AsthmaZone zone, int rescueAttemptsRecent) {
        if (anyRedFlag) return Decision.EMERGENCY;
        if (zone == AsthmaZone.RED) return Decision.HOME_STEPS;
        if (rescueAttemptsRecent >= 2) return Decision.HOME_STEPS;
        return Decision.MONITOR;
    }
}
