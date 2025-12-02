package com.example.smartair;

public class AdherenceDay {
    public String dayName;
    public boolean compliant;
    public int missedCount;
    public boolean isFuture;

    public AdherenceDay(String dayName, boolean compliant, int missedCount, boolean isFuture) {
        this.dayName = dayName;
        this.compliant = compliant;
        this.missedCount = missedCount;
        this.isFuture = isFuture;
    }
}