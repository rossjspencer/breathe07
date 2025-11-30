package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class TrendDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "CHILD_ID";

    private TrendChartView chartView;
    private Button btn7, btn30;
    private String childId;
    private DatabaseReference mDatabase;
    private final List<RescueLog> rescueLogs = new ArrayList<>();
    private int range = 30; // default to bigger view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trend_detail);

        chartView = findViewById(R.id.detailTrendChart);
        btn7 = findViewById(R.id.btnDetail7);
        btn30 = findViewById(R.id.btnDetail30);

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (childId == null) {
            Toast.makeText(this, "No child selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        wireButtons();
        listenForLogs();
    }

    private void wireButtons() {
        btn7.setOnClickListener(v -> {
            range = 7;
            refreshTrend();
        });
        btn30.setOnClickListener(v -> {
            range = 30;
            refreshTrend();
        });
    }

    private void listenForLogs() {
        mDatabase.child("users").child(childId).child("rescueLogs")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescueLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLog log = child.getValue(RescueLog.class);
                            if (log != null) rescueLogs.add(log);
                        }
                        refreshTrend();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void refreshTrend() {
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        List<Integer> points = new ArrayList<>();
        for (int i = range - 1; i >= 0; i--) {
            long start = now - (i + 1) * dayMs;
            long end = now - i * dayMs;
            int count = 0;
            for (RescueLog log : rescueLogs) {
                if (log.timestamp >= start && log.timestamp < end) count++;
            }
            points.add(count);
        }
        chartView.setData(points, range);
        btn7.setEnabled(range != 7);
        btn30.setEnabled(range != 30);
    }
}
