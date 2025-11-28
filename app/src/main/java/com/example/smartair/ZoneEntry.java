package com.example.smartair;

public class ZoneEntry {
    public long timestamp;
    public int score;

    public ZoneEntry() {}

    public ZoneEntry(long timestamp, int score) {
        this.timestamp = timestamp;
        this.score = score;
    }
}
