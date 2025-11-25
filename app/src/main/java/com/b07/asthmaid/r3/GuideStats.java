package com.b07.asthmaid.r3;

public class GuideStats {
    public int totalSessions;
    public int streakDays;
    public String lastSessionDate;

    public GuideStats() {
        this.totalSessions = 0;
        this.streakDays = 0;
        this.lastSessionDate = "";
    }

    public GuideStats(int totalSessions, int streakDays, String lastSessionDate) {
        this.totalSessions = totalSessions;
        this.streakDays = streakDays;
        this.lastSessionDate = lastSessionDate;
    }
}