package com.example.smartair.R4.notify;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class ParentAlertWriter {
    public static void notify(DatabaseReference root, String parentUid, String childId, String type, String message) {
        DatabaseReference a = root.child("alerts").child(parentUid).child("queue").push();
        Map<String,Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("childId", childId);
        alert.put("message", message);
        alert.put("ts", System.currentTimeMillis());
        a.setValue(alert);
    }
}
