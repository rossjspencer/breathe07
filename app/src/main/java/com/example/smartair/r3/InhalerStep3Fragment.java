package com.example.smartair.r3;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class InhalerStep3Fragment extends Fragment {

    private CountDownTimer timer;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_step3, container, false);

        TextView timerText = view.findViewById(R.id.timerText);
        progressBar = view.findViewById(R.id.timerProgress);
        Button finishButton = view.findViewById(R.id.step3FinishButton);
        Button anotherPuffButton = view.findViewById(R.id.step3AnotherPuffButton);

        // initialize progress bar
        progressBar.setProgress(0);

        finishButton.setVisibility(View.INVISIBLE);
        anotherPuffButton.setVisibility(View.INVISIBLE);
        finishButton.setAlpha(0f);
        anotherPuffButton.setAlpha(0f);

        // start timer
        long totalTime = 10000;
        
        timer = new CountDownTimer(totalTime, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.valueOf(millisUntilFinished / 1000 + 1));

                long elapsed = totalTime - millisUntilFinished;
                int progress = (int) ((elapsed * 1000) / totalTime);
                progressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timerText.setText("0");
                progressBar.setProgress(1000);

                finishButton.setVisibility(View.VISIBLE);
                anotherPuffButton.setVisibility(View.VISIBLE);
                
                finishButton.animate().alpha(1f).setDuration(1000).start();
                anotherPuffButton.animate().alpha(1f).setDuration(1000).start();
            }
        }.start();

        finishButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            
            InhalerLogPromptFragment nextFragment = new InhalerLogPromptFragment();
            if (getArguments() != null) {
                nextFragment.setArguments(getArguments());
            }
            
            transaction.replace(R.id.fragment_container, nextFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        anotherPuffButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            
            InhalerWaitFragment fragment = new InhalerWaitFragment();
            if (getArguments() != null) {
                fragment.setArguments(getArguments());
            }
            
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
        }
    }
}