package com.example.garbagescaner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.example.garbagescaner.dialogs.AchievementUnlockDialog;


import com.example.garbagescaner.dialogs.AchievementUnlockDialog;
import com.example.garbagescaner.models.Achievement;
import com.example.garbagescaner.models.ScanResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AchievementManager {
    private static final String TAG = "AchievementManager";
    private static final String PREF_NAME = "achievements_prefs";
    private static final String KEY_ACHIEVEMENTS = "achievements";

    private final Context context;
    private final SharedPreferences preferences;
    private final Gson gson;
    private List<Achievement> achievements;

    // ID достижений
    public static final int ACHIEVEMENT_FIRST_SCAN = 1;
    public static final int ACHIEVEMENT_FIRST_RECYCLE = 2;
    public static final int ACHIEVEMENT_FIVE_RECYCLES = 3;
    public static final int ACHIEVEMENT_TEN_RECYCLES = 4;
    public static final int ACHIEVEMENT_TWENTY_FIVE_RECYCLES = 5;
    public static final int ACHIEVEMENT_FIFTY_RECYCLES = 6;
    public static final int ACHIEVEMENT_HUNDRED_RECYCLES = 7;
    public static final int ACHIEVEMENT_PLASTIC_MASTER = 8;
    public static final int ACHIEVEMENT_PAPER_MASTER = 9;
    public static final int ACHIEVEMENT_GLASS_MASTER = 10;
    public static final int ACHIEVEMENT_METAL_MASTER = 11;
    public static final int ACHIEVEMENT_DIVERSE = 12;

    public AchievementManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        // Загружаем достижения или создаем новые
        loadAchievements();
    }

    private void loadAchievements() {
        String achievementsJson = preferences.getString(KEY_ACHIEVEMENTS, null);

        if (achievementsJson != null) {
            try {
                Type type = new TypeToken<List<Achievement>>() {}.getType();
                achievements = gson.fromJson(achievementsJson, type);
                Log.d(TAG, "Loaded " + achievements.size() + " achievements from preferences");
            } catch (Exception e) {
                Log.e(TAG, "Error loading achievements: " + e.getMessage());
                initDefaultAchievements();
            }
        } else {
            Log.d(TAG, "No saved achievements, initializing defaults");
            initDefaultAchievements();
        }
    }

    private void initDefaultAchievements() {
        achievements = new ArrayList<>();

        // Добавляем достижения с целевыми значениями
        achievements.add(new Achievement(
                ACHIEVEMENT_FIRST_SCAN,
                "Первые шаги",
                "Отсканируйте первый предмет",
                "ic_achievement_default",
                1));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIRST_RECYCLE,
                "Эко-старт",
                "Утилизируйте первый предмет",
                "ic_achievement_default",
                1));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIVE_RECYCLES,
                "Начинающий эколог",
                "Утилизируйте 5 предметов",
                "ic_achievement_default",
                5));

        achievements.add(new Achievement(
                ACHIEVEMENT_TEN_RECYCLES,
                "Эко-энтузиаст",
                "Утилизируйте 10 предметов",
                "ic_achievement_default",
                10));

        achievements.add(new Achievement(
                ACHIEVEMENT_TWENTY_FIVE_RECYCLES,
                "Защитник природы",
                "Утилизируйте 25 предметов",
                "ic_achievement_default",
                25));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIFTY_RECYCLES,
                "Эко-воин",
                "Утилизируйте 50 предметов",
                "ic_achievement_default",
                50));

        achievements.add(new Achievement(
                ACHIEVEMENT_HUNDRED_RECYCLES,
                "Эко-легенда",
                "Утилизируйте 100 предметов",
                "ic_achievement_default",
                100));

        achievements.add(new Achievement(
                ACHIEVEMENT_PLASTIC_MASTER,
                "Мастер пластика",
                "Утилизируйте 15 пластиковых предметов",
                "ic_achievement_default",
                15));

        achievements.add(new Achievement(
                ACHIEVEMENT_PAPER_MASTER,
                "Бумажный гуру",
                "Утилизируйте 15 бумажных предметов",
                "ic_achievement_default",
                15));

        achievements.add(new Achievement(
                ACHIEVEMENT_GLASS_MASTER,
                "Стеклянный маг",
                "Утилизируйте 15 стеклянных предметов",
                "ic_achievement_default",
                15));

        achievements.add(new Achievement(
                ACHIEVEMENT_METAL_MASTER,
                "Металлист",
                "Утилизируйте 15 металлических предметов",
                "ic_achievement_default",
                15));

        achievements.add(new Achievement(
                ACHIEVEMENT_DIVERSE,
                "Эко-разнообразие",
                "Утилизируйте хотя бы по 1 предмету каждого типа",
                "ic_achievement_default",
                4));

        // Сохраняем достижения
        saveAchievements();
    }

    private void saveAchievements() {
        try {
            String achievementsJson = gson.toJson(achievements);
            preferences.edit().putString(KEY_ACHIEVEMENTS, achievementsJson).apply();
            Log.d(TAG, "Achievements saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving achievements: " + e.getMessage());
        }
    }

    public List<Achievement> getAllAchievements() {
        return achievements;
    }

    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }

    public void checkAndUnlockAchievements(List<ScanResult> scanResults) {
        if (scanResults == null || scanResults.isEmpty()) {
            Log.w(TAG, "No scan results to check achievements");
            return;
        }

        Log.d(TAG, "Checking achievements for " + scanResults.size() + " scan results");

        // Подсчет сканирований и утилизированных отходов
        int totalScans = scanResults.size(); // Общее количество сканирований
        int totalRecycled = 0; // Общее количество утилизаций

        // Подсчет по типам утилизированных отходов
        int plasticRecycled = 0;
        int paperRecycled = 0;
        int glassRecycled = 0;
        int metalRecycled = 0;

        // Отслеживание типов для разнообразия
        Set<String> recycledTypes = new HashSet<>();

        // Обновляем достижение за первое сканирование
        if (totalScans > 0) {
            updateAchievementProgress(ACHIEVEMENT_FIRST_SCAN, 1);
        }

        // Подсчет утилизированных отходов
        for (ScanResult scan : scanResults) {
            if (scan.isRecycled()) {
                totalRecycled++;

                String wasteType = scan.getWasteType().toLowerCase();

                if (wasteType.contains("пластик")) {
                    plasticRecycled++;
                    recycledTypes.add("пластик");
                } else if (wasteType.contains("бумаг") || wasteType.contains("картон")) {
                    paperRecycled++;
                    recycledTypes.add("бумага");
                } else if (wasteType.contains("стекл")) {
                    glassRecycled++;
                    recycledTypes.add("стекло");
                } else if (wasteType.contains("метал")) {
                    metalRecycled++;
                    recycledTypes.add("металл");
                } else {
                    recycledTypes.add("прочее");
                }
            }
        }

        Log.d(TAG, "Stats: totalScans=" + totalScans + ", totalRecycled=" + totalRecycled);
        Log.d(TAG, "Recycled by type: plastic=" + plasticRecycled + ", paper=" + paperRecycled +
                ", glass=" + glassRecycled + ", metal=" + metalRecycled +
                ", unique types=" + recycledTypes.size());

        // Обновляем достижения, связанные ТОЛЬКО с утилизацией
        // Первая утилизация
        if (totalRecycled > 0) {
            updateAchievementProgress(ACHIEVEMENT_FIRST_RECYCLE, 1);
        }

        // Количество утилизаций
        updateAchievementProgress(ACHIEVEMENT_FIVE_RECYCLES, totalRecycled);
        updateAchievementProgress(ACHIEVEMENT_TEN_RECYCLES, totalRecycled);
        updateAchievementProgress(ACHIEVEMENT_TWENTY_FIVE_RECYCLES, totalRecycled);
        updateAchievementProgress(ACHIEVEMENT_FIFTY_RECYCLES, totalRecycled);
        updateAchievementProgress(ACHIEVEMENT_HUNDRED_RECYCLES, totalRecycled);

        // По типам отходов
        updateAchievementProgress(ACHIEVEMENT_PLASTIC_MASTER, plasticRecycled);
        updateAchievementProgress(ACHIEVEMENT_PAPER_MASTER, paperRecycled);
        updateAchievementProgress(ACHIEVEMENT_GLASS_MASTER, glassRecycled);
        updateAchievementProgress(ACHIEVEMENT_METAL_MASTER, metalRecycled);

        // Разнообразие типов
        updateAchievementProgress(ACHIEVEMENT_DIVERSE, recycledTypes.size());

        // Сохраняем изменения
        saveAchievements();
    }

    private void updateAchievementProgress(int achievementId, int newValue) {
        Achievement achievement = getAchievementById(achievementId);
        if (achievement != null) {
            Log.d(TAG, "Checking progress for achievement: " + achievement.getTitle() +
                    " (" + achievement.getCurrentValue() + "/" + achievement.getTargetValue() +
                    ") -> new value: " + newValue);

            // Обновляем прогресс и проверяем, было ли разблокировано достижение
            if (achievement.getCurrentValue() < newValue) {
                boolean unlocked = achievement.setCurrentValue(newValue);

                // Если достижение только что разблокировано, показываем уведомление
                if (unlocked) {
                    Log.d(TAG, "Achievement unlocked: " + achievement.getTitle());

                    try {
                        // Показываем красивое уведомление
                        AchievementUnlockDialog dialog = new AchievementUnlockDialog(context);
                        dialog.show(achievement);

                        // Также показываем Toast
                        Toast.makeText(context,
                                "Достижение разблокировано: " + achievement.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing achievement notification: " + e.getMessage());
                    }
                }
            }
        } else {
            Log.e(TAG, "Achievement with ID " + achievementId + " not found");
        }
    }

    private Achievement getAchievementById(int achievementId) {
        for (Achievement achievement : achievements) {
            if (achievement.getId() == achievementId) {
                return achievement;
            }
        }
        return null;
    }

    public Achievement unlockAchievement(int achievementId) {
        Achievement achievement = getAchievementById(achievementId);
        if (achievement != null && !achievement.isUnlocked()) {
            Log.d(TAG, "Unlocking achievement: " + achievement.getTitle());
            achievement.unlock();
            saveAchievements();

            try {
                // Показываем красивое уведомление
                AchievementUnlockDialog dialog = new AchievementUnlockDialog(context);
                dialog.show(achievement);

                // Дополнительно показываем Toast через небольшую задержку
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Toast.makeText(context,
                            "Достижение разблокировано: " + achievement.getTitle(),
                            Toast.LENGTH_SHORT).show();
                }, 500);
            } catch (Exception e) {
                Log.e(TAG, "Error showing achievement notification: " + e.getMessage());
                // Если с диалогом что-то не так, показываем хотя бы Toast
                Toast.makeText(context,
                        "Достижение разблокировано: " + achievement.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }

            return achievement;
        }
        return null;
    }
    public void notifyAchievementUnlocked(Achievement achievement) {
        try {
            // Показываем красивое уведомление
            AchievementUnlockDialog dialog = new AchievementUnlockDialog(context);
            dialog.show(achievement);
        } catch (Exception e) {
            Log.e(TAG, "Error showing achievement notification: " + e.getMessage());
        }
    }

    public void resetAllAchievements() {
        initDefaultAchievements();
        Toast.makeText(context, "Достижения сброшены", Toast.LENGTH_SHORT).show();
    }
}