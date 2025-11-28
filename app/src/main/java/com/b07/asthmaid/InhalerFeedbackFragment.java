package com.b07.asthmaid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.b07.asthmaid.r3.InhalerPostCheckFragment;

public class InhalerFeedbackFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_feedback, container, false);

        Button yesButton = view.findViewById(R.id.feedbackYesButton);
        Button noButton = view.findViewById(R.id.feedbackNoButton);

        yesButton.setOnClickListener(v -> navigateToPostCheck(true));
        noButton.setOnClickListener(v -> navigateToPostCheck(false));

        return view;
    }

    private void navigateToPostCheck(boolean success) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        
        InhalerPostCheckFragment fragment = new InhalerPostCheckFragment();
        Bundle args = new Bundle();
        args.putBoolean("success", success);
        fragment.setArguments(args);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}