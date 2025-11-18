package com.example.smartair;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProviderHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setTextSize(24);
        tv.setText("Welcome Provider!");
        tv.setPadding(50, 50, 50, 50);

        setContentView(tv);
    }
}