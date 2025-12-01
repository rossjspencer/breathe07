package com.example.smartair.r3;

import java.util.HashMap;
import java.util.Map;

public class GuideStats {
    public int totalSessions;
    public int streakDays; // technique streak
    public int controllerStreakDays; // controller adherence streak
    public String lastSessionDate;
    public String accountCreationDate; // "yyyy-MM-dd"
    public Map<String, Boolean> earnedBadges;
    public Map<String, Boolean> pendingNotifications; // badges earned "by parent", waiting to notify child

    public GuideStats() {
        this.totalSessions = 0;
        this.streakDays = 0;
        this.controllerStreakDays = 0;
        this.lastSessionDate = "";
        this.accountCreationDate = "";
        this.earnedBadges = new HashMap<>();
        this.pendingNotifications = new HashMap<>();
    }

    public GuideStats(int totalSessions, int streakDays, String lastSessionDate, String accountCreationDate) {
        this.totalSessions = totalSessions;
        this.streakDays = streakDays;
        this.controllerStreakDays = 0;
        this.lastSessionDate = lastSessionDate;
        this.accountCreationDate = accountCreationDate;
        this.earnedBadges = new HashMap<>();
        this.pendingNotifications = new HashMap<>();
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
    
    public void addPendingNotification(String badgeId) {
        if (pendingNotifications == null) {
            pendingNotifications = new HashMap<>();
        }
        pendingNotifications.put(badgeId, true);
    }
    
    public void clearPendingNotification(String badgeId) {
        if (pendingNotifications != null) {
            pendingNotifications.remove(badgeId);
        }
    }
}