package com.example.garbagescaner.models;

import android.util.Log;

public class Achievement {
    private static final String TAG = "Achievement";
    private int id;
    private String title;
    private String description;
    private String iconResName;
    private boolean unlocked;
    private long unlockedTimestamp;
    private int targetValue;
    private int currentValue;

    public Achievement(int id, String title, String description, String iconResName, int targetValue) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResName = iconResName;
        this.unlocked = false;
        this.unlockedTimestamp = 0;
        this.targetValue = targetValue;
        this.currentValue = 0;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIconResName() {
        return iconResName;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public long getUnlockedTimestamp() {
        return unlockedTimestamp;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public boolean setCurrentValue(int newValue) {
        Log.d(TAG, "Setting progress for " + title + " from " + currentValue + " to " + newValue);

        boolean wasUnlocked = unlocked;

        // Устанавливаем новое значение, но не больше целевого
        this.currentValue = Math.min(newValue, targetValue);

        // Проверяем, достигли ли цели
        if (this.currentValue >= this.targetValue && !this.unlocked) {
            unlock();
            return true; // Достижение было разблокировано
        }

        return false; // Достижение не было разблокировано
    }

    public int getProgressPercentage() {
        if (targetValue <= 0) return 0;
        return Math.min(100, (currentValue * 100) / targetValue);
    }

    public void unlock() {
        Log.d(TAG, "Unlocking achievement: " + title);
        this.unlocked = true;
        this.unlockedTimestamp = System.currentTimeMillis();
        this.currentValue = this.targetValue;
    }
}