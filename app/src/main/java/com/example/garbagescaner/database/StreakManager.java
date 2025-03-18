package com.example.garbagescaner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.garbagescaner.models.StreakData;
import com.google.gson.Gson;

import java.util.Calendar;

public class StreakManager {
    private static final String TAG = "StreakManager";
    private static final String PREF_NAME = "streak_prefs";
    private static final String KEY_STREAK_DATA = "streak_data";

    private final Context context;
    private final SharedPreferences preferences;
    private final Gson gson;
    private StreakData streakData;

    public StreakManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        // Загружаем данные о ударном режиме
        loadStreakData();

        // Проверяем состояние при каждой инициализации
        checkAndUpdateStreak();
    }

    private void loadStreakData() {
        String streakJson = preferences.getString(KEY_STREAK_DATA, null);

        if (streakJson != null) {
            try {
                streakData = gson.fromJson(streakJson, StreakData.class);
                Log.d(TAG, "Loaded streak data: " + streakData);
            } catch (Exception e) {
                Log.e(TAG, "Error loading streak data: " + e.getMessage());
                streakData = new StreakData();
            }
        } else {
            streakData = new StreakData();
        }
    }

    private void saveStreakData() {
        try {
            String streakJson = gson.toJson(streakData);
            preferences.edit().putString(KEY_STREAK_DATA, streakJson).apply();
            Log.d(TAG, "Saved streak data: " + streakData);
        } catch (Exception e) {
            Log.e(TAG, "Error saving streak data: " + e.getMessage());
        }
    }

    // Регистрация утилизации
    public boolean registerRecycling() {
        boolean streakIncreased = false;

        // Получаем текущую дату
        Calendar today = StreakData.getTodayCalendar();
        long todayTime = today.getTimeInMillis();

        // Проверяем, была ли уже утилизация сегодня
        if (!streakData.hasRecycledToday()) {
            // Обновляем дату последней утилизации
            streakData.setLastRecycleDate(todayTime);

            // Проверяем, нужно ли увеличить счетчик
            if (streakData.hasRecycledYesterday() || streakData.getCurrentStreak() == 0) {
                // Увеличиваем счетчик только если вчера тоже была утилизация или это первая утилизация
                streakData.setCurrentStreak(streakData.getCurrentStreak() + 1);
                streakIncreased = true;
                Log.d(TAG, "Streak increased to " + streakData.getCurrentStreak());
            }

            // Обновляем дату проверки
            streakData.setLastCheckDate(todayTime);

            // Сохраняем изменения
            saveStreakData();
        }

        return streakIncreased;
    }

    // Проверка и обновление ударного режима
    private void checkAndUpdateStreak() {
        // Получаем текущую дату
        Calendar today = StreakData.getTodayCalendar();
        Calendar lastCheck = Calendar.getInstance();
        lastCheck.setTimeInMillis(streakData.getLastCheckDate());

        // Убираем время из даты последней проверки
        lastCheck.set(Calendar.HOUR_OF_DAY, 0);
        lastCheck.set(Calendar.MINUTE, 0);
        lastCheck.set(Calendar.SECOND, 0);
        lastCheck.set(Calendar.MILLISECOND, 0);

        // Проверяем, прошел ли день или более с последней проверки
        if (today.getTimeInMillis() > lastCheck.getTimeInMillis()) {
            // Проверяем, была ли утилизация вчера
            if (!streakData.hasRecycledYesterday() && !streakData.hasRecycledToday()) {
                // Если нет утилизации вчера и сегодня, сбрасываем счетчик
                streakData.setCurrentStreak(0);
                Log.d(TAG, "Streak reset to 0 due to missed day");
            }

            // Обновляем дату проверки
            streakData.setLastCheckDate(today.getTimeInMillis());
            saveStreakData();
        }
    }

    // Получить текущее значение ударного режима
    public int getCurrentStreak() {
        return streakData.getCurrentStreak();
    }

    // Проверить, была ли утилизация сегодня
    public boolean hasRecycledToday() {
        return streakData.hasRecycledToday();
    }

    // Сбросить ударный режим (для тестирования)
    public void resetStreak() {
        streakData = new StreakData();
        saveStreakData();
    }
}