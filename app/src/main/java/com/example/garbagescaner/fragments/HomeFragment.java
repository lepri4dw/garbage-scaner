package com.example.garbagescaner.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.garbagescaner.R;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private TextView tvEcoFact;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEcoFact = view.findViewById(R.id.tv_eco_fact);

        // Отображаем первый факт сразу
        showRandomFact();

        // Запускаем периодическую смену фактов каждые 30 секунд
        startFactRotation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Останавливаем ротацию фактов при уничтожении представления
        stopFactRotation();
    }

    private void showRandomFact() {
        if (getActivity() == null) return;

        String[] ecoFacts = getResources().getStringArray(R.array.eco_facts);
        int randomIndex = random.nextInt(ecoFacts.length);

        tvEcoFact.setText(ecoFacts[randomIndex]);
    }

    private void startFactRotation() {
        handler.postDelayed(factRunnable, TimeUnit.SECONDS.toMillis(30));
    }

    private void stopFactRotation() {
        handler.removeCallbacks(factRunnable);
    }

    private final Runnable factRunnable = new Runnable() {
        @Override
        public void run() {
            showRandomFact();
            // Повторяем ротацию
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(30));
        }
    };
}