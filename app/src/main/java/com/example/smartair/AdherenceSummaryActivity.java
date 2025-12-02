package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.smartair.r3.ControllerLogEntry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdherenceSummaryActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private RecyclerView rvCalendar;
    private TextView tvDateSubtitle;
    private TextView tvMonthlySuccess;
    private AdherenceAdapter adapter;
    private CalendarAdapter calendarAdapter;
    private final List<AdherenceDay> dayList = new ArrayList<>();
    private final List<CalendarDay> calendarList = new ArrayList<>();
    private final Map<String, Integer> plannedSchedule = new HashMap<>();
    private final List<ControllerLogEntry> controllerLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adherence_summary);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) {
            Toast.makeText(this, "No child ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        recyclerView = findViewById(R.id.rvAdherence);
        rvCalendar = findViewById(R.id.rvCalendar);
        tvDateSubtitle = findViewById(R.id.tvDateSubtitle);
        tvMonthlySuccess = findViewById(R.id.tvMonthlySuccess);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdherenceAdapter(dayList);
        recyclerView.setAdapter(adapter);

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(calendarList);
        rvCalendar.setAdapter(calendarAdapter);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        tvDateSubtitle.setText("Today is " + sdf.format(new Date()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        mDatabase.child("users").child(childId).child("plannedSchedule")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        plannedSchedule.clear();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String day = d.getKey();
                            Integer val = d.getValue(Integer.class);
                            if (day != null && val != null) {
                                plannedSchedule.put(day, val);
                            }
                        }
                        loadLogs();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadLogs() {
        DatabaseReference logsRef = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");

        logsRef.child("controller").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLogEntry log = child.getValue(ControllerLogEntry.class);
                            if (log != null) controllerLogs.add(log);
                        }
                        calculateAdherence();
                        calculateMonthlyAdherence();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void calculateAdherence() {
        dayList.clear();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(today.getTime());
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        if (cal.after(today)) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Map<String, Integer> dailyCount = new HashMap<>();
        
        for (ControllerLogEntry log : controllerLogs) {
            if (log.timestamp == null) continue;
            try {
                Date d = fullSdf.parse(log.timestamp);
                if (d != null) {
                    String key = sdf.format(d);
                    dailyCount.put(key, dailyCount.getOrDefault(key, 0) + log.doseCount);
                }
            } catch (ParseException e) {}
        }
        
        for (int i = 0; i < 7; i++) {
            boolean isFuture = cal.after(today);
            
            String dayKey = sdf.format(cal.getTime());
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = getDayName(dayOfWeek);
            String schedKey = getDayShortCode(dayOfWeek);
            
            int planned = plannedSchedule.getOrDefault(schedKey, 0);
            int actual = dailyCount.getOrDefault(dayKey, 0);
            
            boolean compliant = (actual >= planned);
            int missed = Math.max(0, planned - actual);
            
            if (planned == 0) {
                compliant = true; 
                missed = 0;
            }
            
            dayList.add(new AdherenceDay(dayName, compliant, missed, isFuture));
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        adapter.notifyDataSetChanged();
    }

    private void calculateMonthlyAdherence() {
        calendarList.clear();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar cal = (Calendar) today.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1); 
        
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // placeholders
        for (int i = 1; i < startDayOfWeek; i++) {
            calendarList.add(new CalendarDay(0, -1)); 
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Map<String, Integer> dailyCount = new HashMap<>();
        
        for (ControllerLogEntry log : controllerLogs) {
            if (log.timestamp == null) continue;
            try {
                Date d = fullSdf.parse(log.timestamp);
                if (d != null) {
                    String key = sdf.format(d);
                    dailyCount.put(key, dailyCount.getOrDefault(key, 0) + log.doseCount);
                }
            } catch (ParseException e) {}
        }
        
        int totalMissed = 0;
        
        for (int i = 1; i <= maxDays; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            
            if (cal.after(today)) {
                calendarList.add(new CalendarDay(i, -1)); 
                continue;
            }
            
            String dayKey = sdf.format(cal.getTime());
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String schedKey = getDayShortCode(dayOfWeek);
            
            int planned = plannedSchedule.getOrDefault(schedKey, 0);
            int actual = dailyCount.getOrDefault(dayKey, 0);
            int missed = Math.max(0, planned - actual);
            
            if (planned == 0) missed = 0;
            if (missed > 0) totalMissed++;
            
            calendarList.add(new CalendarDay(i, missed));
        }
        
        calendarAdapter.notifyDataSetChanged();
        
        if (totalMissed == 0) {
            tvMonthlySuccess.setVisibility(View.VISIBLE);
            rvCalendar.setVisibility(View.VISIBLE); 
        } else {
            tvMonthlySuccess.setVisibility(View.GONE);
            rvCalendar.setVisibility(View.VISIBLE);
        }
    }
    
    private String getDayName(int day) {
        return new java.text.DateFormatSymbols().getWeekdays()[day];
    }
    
    private String getDayShortCode(int day) {
        switch (day) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "Mon";
        }
    }
}