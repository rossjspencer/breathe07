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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_pre_check, container, false);

        RadioGroup radioGroupBreathing = view.findViewById(R.id.radioGroupBreathing);
        RadioGroup radioGroupComparison = view.findViewById(R.id.radioGroupComparison);
        Button nextButton = view.findViewById(R.id.preCheckNextButton);

        radioGroupBreathing.setOnCheckedChangeListener((group, checkedId) -> {
            isBreathingSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupComparison.setOnCheckedChangeListener((group, checkedId) -> {
            isComparisonSelected = true;
            checkEnableButton(nextButton);
        });

        nextButton.setOnClickListener(v -> {
            // navigate to the first step of the guide
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            transaction.replace(R.id.fragment_container, new InhalerStep1Fragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void checkEnableButton(Button button) {
        button.setEnabled(isBreathingSelected && isComparisonSelected);
    }
}