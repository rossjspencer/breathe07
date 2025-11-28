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

    private boolean isBreathingSelected = false;
    private boolean isComparisonSelected = false;
    private boolean isTechniqueSelected = false;
    private boolean isTechniqueSuccess = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_post_check, container, false);

        RadioGroup radioGroupBreathing = view.findViewById(R.id.radioGroupBreathing);
        RadioGroup radioGroupComparison = view.findViewById(R.id.radioGroupComparison);
        RadioGroup radioGroupTechnique = view.findViewById(R.id.radioGroupTechnique);
        Button nextButton = view.findViewById(R.id.postCheckNextButton);

        radioGroupBreathing.setOnCheckedChangeListener((group, checkedId) -> {
            isBreathingSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupComparison.setOnCheckedChangeListener((group, checkedId) -> {
            isComparisonSelected = true;
            checkEnableButton(nextButton);
        });

        radioGroupTechnique.setOnCheckedChangeListener((group, checkedId) -> {
            isTechniqueSelected = true;
            isTechniqueSuccess = (checkedId == R.id.radioTechniqueYes);
            checkEnableButton(nextButton);
        });

        nextButton.setOnClickListener(v -> {
            if (isTechniqueSuccess) {
                navigateToFinish(true);
            } else {
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
        button.setEnabled(isBreathingSelected && isComparisonSelected && isTechniqueSelected);
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