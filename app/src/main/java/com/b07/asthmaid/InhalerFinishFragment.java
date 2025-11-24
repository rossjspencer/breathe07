package com.b07.asthmaid;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class InhalerFinishFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_finish, container, false);

        // confetti library? yes please!
        KonfettiView konfettiView = view.findViewById(R.id.konfettiView);
        Button homeButton = view.findViewById(R.id.finishHomeButton);

        Party party = new PartyFactory(new Emitter(100L, TimeUnit.MILLISECONDS).max(100))
                .spread(360)
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(0f, 30f)
                .position(0.5, 0.3)
                .build();

        konfettiView.start(party);

        homeButton.setOnClickListener(v -> {
            // Return to home screen
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                // Clear back stack to go back to HomeFragment
                getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });

        return view;
    }
}