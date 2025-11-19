////package com.example.smartair;
////
////import android.os.Bundle;
////import android.widget.TextView;
////import androidx.appcompat.app.AppCompatActivity;
////
////import com.google.firebase.auth.FirebaseAuth;
////import com.google.firebase.database.DatabaseReference;
////import com.google.firebase.database.FirebaseDatabase;
////
////import java.util.Random;
////
////public class ChildHomeActivity extends AppCompatActivity {
////    private static final String TAG = "ChildHomeActivity";
////
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////
////        TextView tv = new TextView(this);
////        tv.setTextSize(24);
////        tv.setText("Welcome Child!");
////        tv.setPadding(50, 50, 50, 50);
////
////        setContentView(tv);
////    }
////    private void generatePairingCode() {
////        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
////        StringBuilder code = new StringBuilder();
////        Random rnd = new Random();
////        while (code.length() < 10) { // 6-digit code
////            int index = (int) (rnd.nextFloat() * characters.length());
////            code.append(characters.charAt(index));
////        }
////
////        String uniqueCode = code.toString();
////
////        // CRITICAL: Save this to Firebase so the Parent can find it
////        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
////        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
////
////        mDatabase.child("users").child(currentUserId).child("pairingCode").setValue(uniqueCode)
////                .addOnSuccessListener(aVoid -> {
////                    // Display code to user (e.g., textview.setText(uniqueCode));
////                });
////
////    }
////}
//
//package com.example.smartair;
//
//import android.os.Bundle;
//import android.widget.TextView;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import java.util.Random;
//
//public class ChildHomeActivity extends AppCompatActivity {
//
//    private TextView tvPairingCode;
//    private DatabaseReference mDatabase;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_childhome); // Make sure this XML file exists!
//
//        tvPairingCode = findViewById(R.id.tvPairingCode); // This ID must exist in XML
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//
//        generateAndSavePairingCode();
//    }
//
//    private void generateAndSavePairingCode() {
//        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        StringBuilder codeBuilder = new StringBuilder();
//        Random rnd = new Random();
//
//        // CHANGED TO 6 TO MATCH PARENT REQUIREMENT
//        while (codeBuilder.length() < 6) {
//            int index = (int) (rnd.nextFloat() * characters.length());
//            codeBuilder.append(characters.charAt(index));
//        }
//        String uniqueCode = codeBuilder.toString();
//
//        // Update UI
//        if (tvPairingCode != null) {
//            tvPairingCode.setText(uniqueCode);
//        }
//
//        // Save to Firebase
//        String currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        mDatabase.child("users").child(currentChildId).child("pairingCode").setValue(uniqueCode);
//    }
//}
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
        setContentView(R.layout.activity_child_home); // Make sure this XML file exists!

        tvPairingCode = findViewById(R.id.tvPairingCode); // This ID must exist in XML
        mDatabase = FirebaseDatabase.getInstance().getReference();

        generateAndSavePairingCode();
    }

    private void generateAndSavePairingCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        Random rnd = new Random();

        // CHANGED TO 6 TO MATCH PARENT REQUIREMENT
        while (codeBuilder.length() < 6) {
            int index = (int) (rnd.nextFloat() * characters.length());
            codeBuilder.append(characters.charAt(index));
        }
        String uniqueCode = codeBuilder.toString();

        // Update UI
        if (tvPairingCode != null) {
            tvPairingCode.setText(uniqueCode);
        }

        // Save to Firebase
        String currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(currentChildId).child("pairingCode").setValue(uniqueCode);
    }
}