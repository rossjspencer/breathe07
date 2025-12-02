package com.example.smartair;

public class CalendarDay {
    public int dayOfMonth;
    public int missedCount; // -1 for empty/future, 0 for compliant, > 0 for missed

    public CalendarDay(int dayOfMonth, int missedCount) {
        this.dayOfMonth = dayOfMonth;
        this.missedCount = missedCount;
    }
}