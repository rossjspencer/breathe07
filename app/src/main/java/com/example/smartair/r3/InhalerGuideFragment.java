package com.example.smartair.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class InhalerGuideFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_guide_prompt, container, false);

        Button backButton = view.findViewById(R.id.guideBackButton);
        Button readyButton = view.findViewById(R.id.guideReadyButton);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().finish();
            }
        });

        readyButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            
            InhalerPreCheckFragment nextFragment = new InhalerPreCheckFragment();
            if (getArguments() != null) {
                nextFragment.setArguments(getArguments());
            }
            
            transaction.replace(R.id.fragment_container, nextFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
}