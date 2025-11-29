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
import androidx.fragment.app.FragmentTransaction;

import com.b07.asthmaid.R;

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
            
            if (useSpacer) {
                // go to spacer step 1
                transaction.replace(R.id.fragment_container, new SpacerStep1Fragment()); 
            } else {
                // go to standard step 1
                transaction.replace(R.id.fragment_container, new InhalerStep1Fragment());
            }
            
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void checkEnableButton(Button button) {
        button.setEnabled(isBreathingSelected && isComparisonSelected && isSpacerSelected);
    }
}