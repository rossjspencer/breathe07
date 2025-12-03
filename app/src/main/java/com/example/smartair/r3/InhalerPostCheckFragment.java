package com.example.smartair.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.HomeFragment;
import com.example.smartair.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class InhalerPostCheckFragment extends Fragment {

    private boolean isBreathingSelected = false;
    private boolean isComparisonSelected = false;
    private boolean isTechniqueSelected = false;
    private boolean isTechniqueSuccess = false;
    private boolean isFeelingWorse = false;
    private String currentUserId = "DEFAULT_VALUE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_post_check, container, false);
        
        if (getArguments() != null) {
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        RadioGroup radioGroupBreathing = view.findViewById(R.id.radioGroupBreathing);
        RadioGroup radioGroupComparison = view.findViewById(R.id.radioGroupComparison);
        RadioGroup radioGroupTechnique = view.findViewById(R.id.radioGroupTechnique);
        Button nextButton = view.findViewById(R.id.postCheckNextButton);

        // Initially disable and grey out
        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);

        radioGroupBreathing.setOnCheckedChangeListener((group, checkedId) -> {
            isBreathingSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupComparison.setOnCheckedChangeListener((group, checkedId) -> {
            isComparisonSelected = true;
            isFeelingWorse = (checkedId == R.id.radioWorse);
            checkEnableButton(nextButton);
        });

        radioGroupTechnique.setOnCheckedChangeListener((group, checkedId) -> {
            isTechniqueSelected = true;
            isTechniqueSuccess = (checkedId == R.id.radioTechniqueYes);
            checkEnableButton(nextButton);
        });

        nextButton.setOnClickListener(v -> {
            if (isFeelingWorse) {
                // queue notification
                // we want the parent to see this when they log in
                DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentUserId);
                statsRef.child("pendingNotifications").child("worse_feeling").setValue(true);
            }
            
            if (isTechniqueSuccess) {
                navigateToFinish(true);
            } else {
                // if technique wasn't good, do not give the child the satisfaction of confetti
                // no participation trophies in this house, son
                if (getActivity() != null) {
                    getActivity().finish();
                } else if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
            }
        });

        return view;
    }

    private void checkEnableButton(Button button) {
        boolean enabled = isBreathingSelected && isComparisonSelected && isTechniqueSelected;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void navigateToFinish(boolean success) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        
        InhalerFinishFragment fragment = new InhalerFinishFragment();
        Bundle args = new Bundle();
        args.putBoolean("success", success);
        if (getArguments() != null && getArguments().containsKey("CHILD_ID")) {
            args.putString("CHILD_ID", getArguments().getString("CHILD_ID"));
        }
        fragment.setArguments(args);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}