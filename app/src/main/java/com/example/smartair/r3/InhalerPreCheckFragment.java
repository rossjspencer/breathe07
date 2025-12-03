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
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class InhalerPreCheckFragment extends Fragment {

    private boolean isBreathingSelected = false;
    private boolean isComparisonSelected = false;
    private boolean isSpacerSelected = false;
    private boolean useSpacer = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_pre_check, container, false);

        RadioGroup radioGroupBreathing = view.findViewById(R.id.radioGroupBreathing);
        RadioGroup radioGroupComparison = view.findViewById(R.id.radioGroupComparison);
        RadioGroup radioGroupSpacer = view.findViewById(R.id.radioGroupSpacer);
        Button nextButton = view.findViewById(R.id.preCheckNextButton);

        // initially disable and grey out
        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);

        radioGroupBreathing.setOnCheckedChangeListener((group, checkedId) -> {
            isBreathingSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupComparison.setOnCheckedChangeListener((group, checkedId) -> {
            isComparisonSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupSpacer.setOnCheckedChangeListener((group, checkedId) -> {
            isSpacerSelected = true;
            useSpacer = (checkedId == R.id.radioSpacerYes);
            checkEnableButton(nextButton);
        });

        nextButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            
            Fragment nextFragment;

            // children using spacers can a special step 1 page
            if (useSpacer) {
                nextFragment = new SpacerStep1Fragment();
            } else {
                nextFragment = new InhalerStep1Fragment();
            }
            
            if (getArguments() != null) {
                nextFragment.setArguments(getArguments());
            }
            
            transaction.replace(R.id.fragment_container, nextFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void checkEnableButton(Button button) {
        boolean enabled = isBreathingSelected && isComparisonSelected && isSpacerSelected;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.5f);
    }
}