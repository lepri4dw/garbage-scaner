package com.example.garbagescaner.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.garbagescaner.R;
import com.example.garbagescaner.adapters.EcoTipsAdapter;
import com.example.garbagescaner.database.ScanHistoryManager;
import com.example.garbagescaner.database.StreakManager;
import com.example.garbagescaner.database.AchievementManager;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.EcoTip;
import com.example.garbagescaner.models.StatisticPeriod;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private TextView tvEcoFact;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private RecyclerView recyclerViewTips;

    // Добавлены менеджеры
    private ScanHistoryManager historyManager;
    private AchievementManager achievementManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация компонентов
        tvEcoFact = view.findViewById(R.id.tv_eco_fact);
        recyclerViewTips = view.findViewById(R.id.recyclerViewTips);

        // Отображаем первый факт
        showRandomFact();

        // Запускаем периодическую смену фактов
        startFactRotation();

        // Обновляем виджет ударного режима
        updateStreakWidget(view);

        // Загружаем эко-советы
        setupEcoTips();

        // Настраиваем кнопки быстрых действий
        setupActionButtons(view);

        updateStatisticsCounters(view);
    }

    private void updateStatisticsCounters(View view) {
        TextView tvTotalCount = view.findViewById(R.id.tvTotalCount);
        TextView tvAchievementsCount = view.findViewById(R.id.tvAchievementsCount);

        // Получаем менеджеры для получения данных
        ScanHistoryManager historyManager = new ScanHistoryManager(requireContext());
        AchievementManager achievementManager = new AchievementManager(requireContext());

        // Устанавливаем значения счетчиков
        int totalRecycled = historyManager.getTotalRecycledCount(StatisticPeriod.ALL_TIME);
        int achievementsCount = achievementManager.getUnlockedAchievements().size();

        tvTotalCount.setText(String.valueOf(totalRecycled));
        tvAchievementsCount.setText(String.valueOf(achievementsCount));
    }

    private void setupActionButtons(View view) {
        View actionScan = view.findViewById(R.id.actionScan);
        View actionMap = view.findViewById(R.id.actionMap);

        // Обработчик для кнопки сканирования
        actionScan.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Переключаемся на вкладку сканера
                BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.navigation_scanner);
            }
        });

        // Обработчик для кнопки карты пунктов приема
        actionMap.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), MapActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Останавливаем ротацию фактов
        stopFactRotation();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null) {
            updateStatisticsCounters(getView());
        }

        // Обновляем факт
        showRandomFact();

        // Обновляем виджет ударного режима
        updateStreakWidget(getView());
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

    private void updateStreakWidget(View rootView) {
        // Находим элементы виджета
        if (rootView == null) return;

        TextView tvStreakCount = rootView.findViewById(R.id.tvStreakCount);
        TextView tvStreakStatus = rootView.findViewById(R.id.tvStreakStatus);
        ImageView ivStreakIcon = rootView.findViewById(R.id.ivStreakIcon);

        if (tvStreakCount == null || tvStreakStatus == null || ivStreakIcon == null) return;

        // Получаем данные о ударном режиме
        StreakManager streakManager = new StreakManager(requireContext());
        int currentStreak = streakManager.getCurrentStreak();
        boolean recycledToday = streakManager.hasRecycledToday();

        // Обновляем счетчик
        tvStreakCount.setText(formatStreakCount(currentStreak));

        // Обновляем статус
        if (recycledToday) {
            tvStreakStatus.setText("Выполнено!");
            tvStreakStatus.setBackgroundResource(R.drawable.pill_background);
        } else {
            tvStreakStatus.setText("Сканировать!");
            tvStreakStatus.setBackgroundResource(R.drawable.pill_background);
        }

        // Анимация иконки для активного ударного режима
        if (currentStreak > 0) {
            // Пульсирующая анимация
            Animation pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
            ivStreakIcon.startAnimation(pulseAnimation);
        }
    }

    private String formatStreakCount(int count) {
        String suffix;
        if (count % 10 == 1 && count % 100 != 11) {
            suffix = " день подряд";
        } else if ((count % 10 == 2 || count % 10 == 3 || count % 10 == 4) &&
                !(count % 100 == 12 || count % 100 == 13 || count % 100 == 14)) {
            suffix = " дня подряд";
        } else {
            suffix = " дней подряд";
        }
        return count + suffix;
    }

    private void setupEcoTips() {
        // Создаем список эко-советов
        List<EcoTip> tips = new ArrayList<>();

        // Добавляем несколько советов
        tips.add(new EcoTip(R.drawable.tip_reusable_bags, "Многоразовые сумки",
                "Используйте многоразовые сумки вместо пластиковых пакетов при покупках"));

        tips.add(new EcoTip(R.drawable.tip_water_bottle, "Своя бутылка",
                "Носите с собой многоразовую бутылку для воды"));

        tips.add(new EcoTip(R.drawable.tip_sort_waste, "Сортировка",
                "Сортируйте отходы по типам для более эффективной переработки"));

        tips.add(new EcoTip(R.drawable.tip_energy_saving, "Экономия энергии",
                "Выключайте свет и электроприборы, когда они не используются"));

        tips.add(new EcoTip(R.drawable.tip_food_waste, "Пищевые отходы",
                "Компостируйте пищевые отходы или используйте их повторно"));

        // Создаем и устанавливаем адаптер
        EcoTipsAdapter adapter = new EcoTipsAdapter(requireContext(), tips);
        recyclerViewTips.setAdapter(adapter);
    }
}