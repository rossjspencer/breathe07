package com.example.smartair.R4.logic;

import com.example.smartair.R4.model.AsthmaZone;

public class ZoneCalculator {

    public AsthmaZone computeZone(int pefValue, int personalBest) {
        if (personalBest <= 0) {
            return AsthmaZone.UNKNOWN;
        }

        double ratio = (double) pefValue / personalBest;

        if (ratio >= 0.80) {
            return AsthmaZone.GREEN;
        } else if (ratio >= 0.50) {
            return AsthmaZone.YELLOW;
        } else {
            return AsthmaZone.RED;
        }
    }
}
