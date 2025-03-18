package com.example.garbagescaner.models;

public class Achievement {
    private int id;
    private String title;
    private String description;
    private String iconResName;
    private boolean unlocked;
    private long unlockedTimestamp;

    public Achievement(int id, String title, String description, String iconResName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResName = iconResName;
        this.unlocked = false;
        this.unlockedTimestamp = 0;
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

    public void unlock() {
        this.unlocked = true;
        this.unlockedTimestamp = System.currentTimeMillis();
    }
}