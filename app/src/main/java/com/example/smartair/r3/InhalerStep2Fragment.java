package com.example.smartair.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;
import com.bumptech.glide.Glide;

public class InhalerStep2Fragment extends Fragment {

    private boolean isSpacer = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_step2, container, false);

        if (getArguments() != null) {
            isSpacer = getArguments().getBoolean("isSpacer", false);
        }

        Button nextButton = view.findViewById(R.id.step2NextButton);
        ImageView step2Image = view.findViewById(R.id.step2Image);

        // Load GIF using Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.inhaler_press)
                .into(step2Image);

        nextButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            
            InhalerStep3Fragment nextFragment = new InhalerStep3Fragment();
            Bundle args = new Bundle();
            args.putBoolean("isSpacer", isSpacer);
            if (getArguments() != null && getArguments().containsKey("CHILD_ID")) {
                args.putString("CHILD_ID", getArguments().getString("CHILD_ID"));
            }
            nextFragment.setArguments(args);
            
            transaction.replace(R.id.fragment_container, nextFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
}