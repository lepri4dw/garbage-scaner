package com.example.garbagescaner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.garbagescaner.models.Achievement;
import com.example.garbagescaner.models.ScanResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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

        // Загружаем достижения или создаем новые, если их еще нет
        loadAchievements();
    }

    private void loadAchievements() {
        String achievementsJson = preferences.getString(KEY_ACHIEVEMENTS, null);

        if (achievementsJson != null) {
            try {
                Type type = new TypeToken<List<Achievement>>() {}.getType();
                achievements = gson.fromJson(achievementsJson, type);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке достижений: " + e.getMessage());
                initDefaultAchievements();
            }
        } else {
            initDefaultAchievements();
        }
    }

    private void initDefaultAchievements() {
        achievements = new ArrayList<>();

        // Инициализация достижений
        achievements.add(new Achievement(
                ACHIEVEMENT_FIRST_SCAN,
                "Первые шаги",
                "Отсканируйте первый предмет",
                "ic_achievement_first_scan"));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIRST_RECYCLE,
                "Эко-старт",
                "Утилизируйте первый предмет",
                "ic_achievement_first_recycle"));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIVE_RECYCLES,
                "Начинающий эколог",
                "Утилизируйте 5 предметов",
                "ic_achievement_five_recycles"));

        achievements.add(new Achievement(
                ACHIEVEMENT_TEN_RECYCLES,
                "Эко-энтузиаст",
                "Утилизируйте 10 предметов",
                "ic_achievement_ten_recycles"));

        achievements.add(new Achievement(
                ACHIEVEMENT_TWENTY_FIVE_RECYCLES,
                "Защитник природы",
                "Утилизируйте 25 предметов",
                "ic_achievement_twenty_five_recycles"));

        achievements.add(new Achievement(
                ACHIEVEMENT_FIFTY_RECYCLES,
                "Эко-воин",
                "Утилизируйте 50 предметов",
                "ic_achievement_fifty_recycles"));

        achievements.add(new Achievement(
                ACHIEVEMENT_HUNDRED_RECYCLES,
                "Эко-легенда",
                "Утилизируйте 100 предметов",
                "ic_achievement_hundred_recycles"));

        achievements.add(new Achievement(
                ACHIEVEMENT_PLASTIC_MASTER,
                "Мастер пластика",
                "Утилизируйте 15 пластиковых предметов",
                "ic_achievement_plastic_master"));

        achievements.add(new Achievement(
                ACHIEVEMENT_PAPER_MASTER,
                "Бумажный гуру",
                "Утилизируйте 15 бумажных предметов",
                "ic_achievement_paper_master"));

        achievements.add(new Achievement(
                ACHIEVEMENT_GLASS_MASTER,
                "Стеклянный маг",
                "Утилизируйте 15 стеклянных предметов",
                "ic_achievement_glass_master"));

        achievements.add(new Achievement(
                ACHIEVEMENT_METAL_MASTER,
                "Металлист",
                "Утилизируйте 15 металлических предметов",
                "ic_achievement_metal_master"));

        achievements.add(new Achievement(
                ACHIEVEMENT_DIVERSE,
                "Эко-разнообразие",
                "Утилизируйте хотя бы по 1 предмету каждого типа",
                "ic_achievement_diverse"));

        // Сохраняем инициализированные достижения
        saveAchievements();
    }

    private void saveAchievements() {
        try {
            String achievementsJson = gson.toJson(achievements);
            preferences.edit().putString(KEY_ACHIEVEMENTS, achievementsJson).apply();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении достижений: " + e.getMessage());
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

    // Проверка и разблокировка достижений
    public void checkAndUnlockAchievements(List<ScanResult> scanResults) {
        boolean achievementUnlocked = false;

        // Подсчет утилизированных отходов
        int totalRecycled = 0;
        int plasticRecycled = 0;
        int paperRecycled = 0;
        int glassRecycled = 0;
        int metalRecycled = 0;
        boolean hasPlastic = false;
        boolean hasPaper = false;
        boolean hasGlass = false;
        boolean hasMetal = false;
        boolean hasOther = false;

        for (ScanResult scan : scanResults) {
            // Проверяем первое сканирование
            if (!isAchievementUnlocked(ACHIEVEMENT_FIRST_SCAN)) {
                unlockAchievement(ACHIEVEMENT_FIRST_SCAN);
                achievementUnlocked = true;
            }

            // Подсчет по типам если утилизировано
            if (scan.isRecycled()) {
                totalRecycled++;

                String wasteType = scan.getWasteType().toLowerCase();

                if (wasteType.contains("пластик")) {
                    plasticRecycled++;
                    hasPlastic = true;
                } else if (wasteType.contains("бумаг") || wasteType.contains("картон")) {
                    paperRecycled++;
                    hasPaper = true;
                } else if (wasteType.contains("стекл")) {
                    glassRecycled++;
                    hasGlass = true;
                } else if (wasteType.contains("метал")) {
                    metalRecycled++;
                    hasMetal = true;
                } else {
                    hasOther = true;
                }
            }
        }

        // Проверка достижений по количеству утилизаций
        if (totalRecycled > 0 && !isAchievementUnlocked(ACHIEVEMENT_FIRST_RECYCLE)) {
            unlockAchievement(ACHIEVEMENT_FIRST_RECYCLE);
            achievementUnlocked = true;
        }

        if (totalRecycled >= 5 && !isAchievementUnlocked(ACHIEVEMENT_FIVE_RECYCLES)) {
            unlockAchievement(ACHIEVEMENT_FIVE_RECYCLES);
            achievementUnlocked = true;
        }

        if (totalRecycled >= 10 && !isAchievementUnlocked(ACHIEVEMENT_TEN_RECYCLES)) {
            unlockAchievement(ACHIEVEMENT_TEN_RECYCLES);
            achievementUnlocked = true;
        }

        if (totalRecycled >= 25 && !isAchievementUnlocked(ACHIEVEMENT_TWENTY_FIVE_RECYCLES)) {
            unlockAchievement(ACHIEVEMENT_TWENTY_FIVE_RECYCLES);
            achievementUnlocked = true;
        }

        if (totalRecycled >= 50 && !isAchievementUnlocked(ACHIEVEMENT_FIFTY_RECYCLES)) {
            unlockAchievement(ACHIEVEMENT_FIFTY_RECYCLES);
            achievementUnlocked = true;
        }

        if (totalRecycled >= 100 && !isAchievementUnlocked(ACHIEVEMENT_HUNDRED_RECYCLES)) {
            unlockAchievement(ACHIEVEMENT_HUNDRED_RECYCLES);
            achievementUnlocked = true;
        }

        // Проверка достижений по типам отходов
        if (plasticRecycled >= 15 && !isAchievementUnlocked(ACHIEVEMENT_PLASTIC_MASTER)) {
            unlockAchievement(ACHIEVEMENT_PLASTIC_MASTER);
            achievementUnlocked = true;
        }

        if (paperRecycled >= 15 && !isAchievementUnlocked(ACHIEVEMENT_PAPER_MASTER)) {
            unlockAchievement(ACHIEVEMENT_PAPER_MASTER);
            achievementUnlocked = true;
        }

        if (glassRecycled >= 15 && !isAchievementUnlocked(ACHIEVEMENT_GLASS_MASTER)) {
            unlockAchievement(ACHIEVEMENT_GLASS_MASTER);
            achievementUnlocked = true;
        }

        if (metalRecycled >= 15 && !isAchievementUnlocked(ACHIEVEMENT_METAL_MASTER)) {
            unlockAchievement(ACHIEVEMENT_METAL_MASTER);
            achievementUnlocked = true;
        }

        // Проверка на разнообразие
        if (hasPlastic && hasPaper && hasGlass && hasMetal && !isAchievementUnlocked(ACHIEVEMENT_DIVERSE)) {
            unlockAchievement(ACHIEVEMENT_DIVERSE);
            achievementUnlocked = true;
        }

        // Если было разблокировано хотя бы одно достижение, сохраняем
        if (achievementUnlocked) {
            saveAchievements();
        }
    }

    private boolean isAchievementUnlocked(int achievementId) {
        for (Achievement achievement : achievements) {
            if (achievement.getId() == achievementId) {
                return achievement.isUnlocked();
            }
        }
        return false;
    }

    public Achievement unlockAchievement(int achievementId) {
        for (Achievement achievement : achievements) {
            if (achievement.getId() == achievementId && !achievement.isUnlocked()) {
                achievement.unlock();

                // Уведомляем пользователя
                Toast.makeText(context,
                        "Достижение разблокировано: " + achievement.getTitle(),
                        Toast.LENGTH_SHORT).show();

                return achievement;
            }
        }
        return null;
    }
}