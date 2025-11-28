package com.b07.asthmaid.r3;

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

import com.b07.asthmaid.HomeFragment;
import com.b07.asthmaid.R;

public class InhalerPostCheckFragment extends Fragment {

    private boolean success;
    private boolean isBreathingSelected = false;
    private boolean isComparisonSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_post_check, container, false);

        if (getArguments() != null) {
            success = getArguments().getBoolean("success", false);
        }

        RadioGroup radioGroupBreathing = view.findViewById(R.id.radioGroupBreathing);
        RadioGroup radioGroupComparison = view.findViewById(R.id.radioGroupComparison);
        Button nextButton = view.findViewById(R.id.postCheckNextButton);

        radioGroupBreathing.setOnCheckedChangeListener((group, checkedId) -> {
            isBreathingSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupComparison.setOnCheckedChangeListener((group, checkedId) -> {
            isComparisonSelected = true;
            checkEnableButton(nextButton);
        });

        nextButton.setOnClickListener(v -> {
            if (success) {
                // navigate to well done screen
                navigateToFinish(true);
            } else {
                // go back to home
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        return view;
    }

    private void checkEnableButton(Button button) {
        button.setEnabled(isBreathingSelected && isComparisonSelected);
    }

    private void navigateToFinish(boolean success) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        
        InhalerFinishFragment fragment = new InhalerFinishFragment();
        Bundle args = new Bundle();
        args.putBoolean("success", success);
        fragment.setArguments(args);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}