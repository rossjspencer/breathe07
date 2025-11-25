package com.b07.asthmaid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.b07.asthmaid.r3.BadgesFragment;
import com.b07.asthmaid.r3.InhalerGuideFragment;
import com.b07.asthmaid.r3.InventoryFragment;
import com.b07.asthmaid.r3.MedicineLogFragment;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

        Button buttonMedicineLog = view.findViewById(R.id.buttonMedicineLog);
        Button buttonInventory = view.findViewById(R.id.buttonInventory);
        Button buttonGuide = view.findViewById(R.id.buttonGuide);
        Button buttonBadges = view.findViewById(R.id.buttonBadges);

        buttonInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { loadFragment(new InventoryFragment()); }
        });

        buttonMedicineLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { loadFragment(new MedicineLogFragment()); }
        });

        buttonGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new InhalerGuideFragment());
            }
        });

        buttonBadges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new BadgesFragment());
            }
        });

        return view;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
