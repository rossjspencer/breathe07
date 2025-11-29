package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class InviteProviderActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;
    private TextView tvCode, tvExpiry;
    private EditText etProviderEmail;
    private Button btnGenerate, btnRevoke;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_provider);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) { finish(); return; }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvCode = findViewById(R.id.tv_invite_code);
        tvExpiry = findViewById(R.id.tv_expiry);
        etProviderEmail = findViewById(R.id.et_provider_email);
        btnGenerate = findViewById(R.id.btn_generate_code);
        btnRevoke = findViewById(R.id.btn_revoke_code);

        loadActiveInvite();

        btnGenerate.setOnClickListener(v -> generateCode());
        btnRevoke.setOnClickListener(v -> revokeCode());
    }

    private void loadActiveInvite() {
        mDatabase.child("users").child(childId).child("activeInviteCode")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String code = snapshot.getValue(String.class);
                            tvCode.setText(code);
                            btnGenerate.setEnabled(false); // Already has one, prevent duplicates
                            btnRevoke.setVisibility(View.VISIBLE);
                        } else {
                            tvCode.setText("---");
                            btnGenerate.setEnabled(true);
                            btnRevoke.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void generateCode() {
        // Check Email Input
        String email = etProviderEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter the provider's email first.", Toast.LENGTH_SHORT).show();
            return;
        }

        //  Generate 6-char code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        while (sb.length() < 6) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        String code = sb.toString();

        //  Create Invite Object
        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("childId", childId);
        long expiryTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        inviteData.put("expiresAt", expiryTime);

        // Save to 'invitations' node
        mDatabase.child("invitations").child(code).setValue(inviteData)
                .addOnSuccessListener(aVoid -> {
                    // Save reference in Child's node so Parent sees it
                    mDatabase.child("users").child(childId).child("activeInviteCode").setValue(code);

                    tvCode.setText(code);
                    btnGenerate.setEnabled(false);
                    btnRevoke.setVisibility(View.VISIBLE);

                    // Launch Email Intent
                    sendEmailIntent(email, code);
                });
    }

    private void sendEmailIntent(String email, String code) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822"); // Ensures only email apps handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "SMART AIR: Provider Access Invite");
        intent.putExtra(Intent.EXTRA_TEXT, "A parent has invited you to view their child's asthma logs on the SMART AIR app.\n\n" +
                "Access Code: " + code + "\n\n" +
                "This code expires in 7 days.\n" +
                "Please log in to your Provider Dashboard and enter this code to link the patient.");

        try {
            startActivity(Intent.createChooser(intent, "Send Email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void revokeCode() {
        String currentCode = tvCode.getText().toString();
        if (currentCode.equals("---")) return;

        // Delete from 'invitations'
        mDatabase.child("invitations").child(currentCode).removeValue();

        // Delete reference from 'users'
        mDatabase.child("users").child(childId).child("activeInviteCode").removeValue()
                .addOnSuccessListener(aVoid -> {
                    tvCode.setText("---");
                    btnGenerate.setEnabled(true);
                    btnRevoke.setVisibility(View.GONE);
                    Toast.makeText(this, "Code Revoked. Provider access removed.", Toast.LENGTH_SHORT).show();
                });
    }
}