package com.b07.asthmaid.r3;

import java.util.HashMap;
import java.util.Map;

public class GuideStats {
    public int totalSessions;
    public int streakDays;
    public String lastSessionDate;
    public Map<String, Boolean> earnedBadges;

    public GuideStats() {
        this.totalSessions = 0;
        this.streakDays = 0;
        this.lastSessionDate = "";
        this.earnedBadges = new HashMap<>();
    }

    public GuideStats(int totalSessions, int streakDays, String lastSessionDate) {
        this.totalSessions = totalSessions;
        this.streakDays = streakDays;
        this.lastSessionDate = lastSessionDate;
        this.earnedBadges = new HashMap<>();
    }
    
    public boolean hasBadge(String badgeId) {
        return earnedBadges != null && Boolean.TRUE.equals(earnedBadges.get(badgeId));
    }
    
    public void earnBadge(String badgeId) {
        if (earnedBadges == null) {
            earnedBadges = new HashMap<>();
        }
        earnedBadges.put(badgeId, true);
    }
}