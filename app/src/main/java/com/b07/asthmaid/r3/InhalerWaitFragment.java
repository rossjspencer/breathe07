package com.b07.asthmaid.r3;

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

import com.b07.asthmaid.R;

public class InhalerWaitFragment extends Fragment {

    private CountDownTimer timer;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_wait, container, false);

        TextView timerText = view.findViewById(R.id.waitTimerText);
        progressBar = view.findViewById(R.id.waitTimerProgress);
        Button nextButton = view.findViewById(R.id.waitNextButton);

        progressBar.setProgress(0);

        nextButton.setVisibility(View.INVISIBLE);
        nextButton.setAlpha(0f);

        long totalTime = 30000;

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

                nextButton.setVisibility(View.VISIBLE);
                nextButton.animate().alpha(1f).setDuration(1000).start();
            }
        }.start();

        nextButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            transaction.replace(R.id.fragment_container, new InhalerRepeatDoseFragment());
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