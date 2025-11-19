package com.example.smartair;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Random;

public class ChildHomeActivity extends AppCompatActivity {

    private TextView tvPairingCode;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        // Make sure this ID matches your XML!
        tvPairingCode = findViewById(R.id.tvPairingCode);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        generateAndSavePairingCode();
    }

    private void generateAndSavePairingCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        Random rnd = new Random();

        while (codeBuilder.length() < 6) {
            int index = (int) (rnd.nextFloat() * characters.length());
            codeBuilder.append(characters.charAt(index));
        }
        String uniqueCode = codeBuilder.toString();

        if (tvPairingCode != null) {
            tvPairingCode.setText(uniqueCode);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase.child("users").child(currentChildId).child("pairingCode").setValue(uniqueCode);
        }
    }
}