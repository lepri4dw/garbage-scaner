package com.example.garbagescaner.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.garbagescaner.R;
import com.example.garbagescaner.adapters.AchievementsAdapter;
import com.example.garbagescaner.adapters.ScanHistoryAdapter;
import com.example.garbagescaner.database.AchievementManager;
import com.example.garbagescaner.database.ScanHistoryManager;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.Achievement;
import com.example.garbagescaner.models.ScanResult;
import com.example.garbagescaner.models.StatisticPeriod;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment implements ScanHistoryAdapter.OnHistoryItemClickListener {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View historyView;
    private View statisticsView;
    private View achievementsView;

    private RecyclerView recyclerViewHistory;
    private TextView emptyHistoryView;
    private ScanHistoryAdapter historyAdapter;

    private Spinner periodSpinner;
    private PieChart pieChart;
    private TextView tvTotalItems;
    private TextView tvTotalCost;

    private RecyclerView recyclerViewAchievements;
    private TextView tvAchievementsCount;

    private ScanHistoryManager historyManager;
    private AchievementManager achievementManager;
    private StatisticPeriod currentPeriod = StatisticPeriod.ALL_TIME;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация менеджеров
        historyManager = new ScanHistoryManager(requireContext());
        achievementManager = new AchievementManager(requireContext());

        // Инициализация вкладок
        setupTabLayout(view);

        // Получаем LayoutInflater
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // Инициализация view для каждой вкладки
        initializeTabViews(inflater);

        // Настройка ViewPager с адаптером
        setupViewPager();

        // Загружаем историю при создании фрагмента
        loadHistory();

        // Загружаем статистику и достижения
        loadStatistics();
        loadAchievements();
    }

    private void setupTabLayout(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void initializeTabViews(LayoutInflater inflater) {
        // Инициализация view для вкладки Истории
        historyView = inflater.inflate(R.layout.tab_history, null);
        recyclerViewHistory = historyView.findViewById(R.id.recyclerViewHistory);
        emptyHistoryView = historyView.findViewById(R.id.emptyView);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Инициализация view для вкладки Статистики
        statisticsView = inflater.inflate(R.layout.tab_statistics, null);
        periodSpinner = statisticsView.findViewById(R.id.periodSpinner);
        pieChart = statisticsView.findViewById(R.id.pieChart);
        tvTotalItems = statisticsView.findViewById(R.id.tvTotalItems);
        tvTotalCost = statisticsView.findViewById(R.id.tvTotalCost);
        setupPeriodSpinner();

        // Инициализация view для вкладки Достижений
        achievementsView = inflater.inflate(R.layout.tab_achievements, null);
        recyclerViewAchievements = achievementsView.findViewById(R.id.recyclerViewAchievements);
        tvAchievementsCount = achievementsView.findViewById(R.id.tvAchievementsCount);
        recyclerViewAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupViewPager() {
        List<View> tabViews = new ArrayList<>();
        tabViews.add(historyView);
        tabViews.add(statisticsView);
        tabViews.add(achievementsView);

        List<String> tabTitles = new ArrayList<>();
        tabTitles.add("История");
        tabTitles.add("Статистика");
        tabTitles.add("Достижения");

        TabPagerAdapter adapter = new TabPagerAdapter(tabViews);
        viewPager.setAdapter(adapter);

        // Связывание ViewPager2 с TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabTitles.get(position))).attach();
    }

    private void setupPeriodSpinner() {
        StatisticPeriod[] periods = StatisticPeriod.values();
        String[] periodNames = new String[periods.length];

        for (int i = 0; i < periods.length; i++) {
            periodNames[i] = periods[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, periodNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPeriod = StatisticPeriod.values()[position];
                loadStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем историю, статистику и достижения при возвращении к фрагменту
        loadHistory();
        loadStatistics();
        loadAchievements();
    }

    private void loadHistory() {
        List<ScanResult> history = historyManager.getAllScanResults();

        if (history.isEmpty()) {
            recyclerViewHistory.setVisibility(View.GONE);
            emptyHistoryView.setVisibility(View.VISIBLE);
        } else {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            emptyHistoryView.setVisibility(View.GONE);

            historyAdapter = new ScanHistoryAdapter(requireContext(), history, this);
            recyclerViewHistory.setAdapter(historyAdapter);
        }
    }

    private void loadStatistics() {
        // Получение статистики за выбранный период
        Map<String, Integer> countByType = historyManager.getRecycledCountByType(currentPeriod);
        int totalItems = historyManager.getTotalRecycledCount(currentPeriod);

        // Подсчет общего количества
        int totalCount = 0;
        for (int count : countByType.values()) {
            totalCount += count;
        }

        // Установка общих значений
        tvTotalItems.setText(String.format("Всего утилизировано: %d шт.", totalItems));

        // Вместо общей стоимости показываем топ категорий
        String topCategories = getTopCategories(countByType);
        tvTotalCost.setText(topCategories);

        // Настройка диаграммы по типам отходов
        setupPieChart(countByType);
    }

    private String getTopCategories(Map<String, Integer> countByType) {
        // Сортируем категории по количеству (от большего к меньшему)
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(countByType.entrySet());
        sortedEntries.removeIf(entry -> entry.getValue() == 0); // Удаляем нулевые значения

        if (sortedEntries.isEmpty()) {
            return "Нет данных за выбранный период";
        }

        sortedEntries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        // Берем максимум 3 категории для отображения
        int maxToShow = Math.min(3, sortedEntries.size());
        StringBuilder result = new StringBuilder("Топ категорий: ");

        for (int i = 0; i < maxToShow; i++) {
            if (i > 0) result.append(", ");
            Map.Entry<String, Integer> entry = sortedEntries.get(i);
            result.append(entry.getKey()).append(" (").append(entry.getValue()).append(" шт.)");
        }

        return result.toString();
    }

    private void setupPieChart(Map<String, Integer> countByType) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.card_background));
        pieChart.setTransparentCircleRadius(61f);

        List<PieEntry> entries = new ArrayList<>();

        // Добавляем только ненулевые значения
        for (Map.Entry<String, Integer> entry : countByType.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            // Если нет данных, показываем заглушку
            pieChart.setNoDataText("Нет данных за выбранный период");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Типы отходов");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Цвета для категорий
        List<Integer> colors = new ArrayList<>();
        for (int color : ColorTemplate.MATERIAL_COLORS) {
            colors.add(color);
        }
        for (int color : ColorTemplate.VORDIPLOM_COLORS) {
            colors.add(color);
        }
        dataSet.setColors(colors);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieData.setValueTextSize(11f);
        pieData.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        pieChart.setData(pieData);
        pieChart.highlightValues(null);
        pieChart.invalidate();
        pieChart.animateY(1400);
    }

    private void loadAchievements() {
        List<Achievement> allAchievements = achievementManager.getAllAchievements();
        List<Achievement> unlockedAchievements = achievementManager.getUnlockedAchievements();

        // Обновление счетчика достижений
        tvAchievementsCount.setText(String.format("Получено %d из %d",
                unlockedAchievements.size(), allAchievements.size()));

        if (recyclerViewAchievements.getAdapter() == null) {
            AchievementsAdapter adapter = new AchievementsAdapter(requireContext(), allAchievements);
            recyclerViewAchievements.setAdapter(adapter);
        } else {
            // Если адаптер уже существует, просто обновляем данные
            ((AchievementsAdapter) recyclerViewAchievements.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(ScanResult scanResult) {
        // При клике на элемент истории открываем карту
        Intent intent = new Intent(requireContext(), MapActivity.class);
        intent.putExtra("waste_type", scanResult.getWasteType());
        startActivity(intent);
    }

    @Override
    public void onRecycleClick(ScanResult scanResult, int position) {
        // При клике на кнопку утилизации маркируем элемент как утилизированный
        historyManager.markAsRecycled(position);

        // Обновляем интерфейс
        Toast.makeText(requireContext(), "Отмечено как утилизированное!", Toast.LENGTH_SHORT).show();

        // Обновляем списки
        loadHistory();
        loadStatistics();
        loadAchievements();
    }

    // Класс-адаптер для ViewPager
    private static class TabPagerAdapter extends RecyclerView.Adapter<TabPagerAdapter.TabViewHolder> {

        private final List<View> views;

        public TabPagerAdapter(List<View> views) {
            this.views = views;
        }

        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Создаем контейнер для view
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new TabViewHolder(frameLayout);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            // Удаляем предыдущее view, если есть
            ((ViewGroup) holder.itemView).removeAllViews();

            // Если view уже было в другом родителе, удаляем его оттуда
            ViewParent parent = views.get(position).getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(views.get(position));
            }

            // Добавляем view в контейнер
            ((ViewGroup) holder.itemView).addView(views.get(position));
        }

        @Override
        public int getItemCount() {
            return views.size();
        }

        static class TabViewHolder extends RecyclerView.ViewHolder {
            public TabViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}