package com.example.garbagescaner.models;

public enum StatisticPeriod {
    TODAY("За сегодня"),
    WEEK("За неделю"),
    MONTH("За месяц"),
    ALL_TIME("За все время");

    private final String displayName;

    StatisticPeriod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}