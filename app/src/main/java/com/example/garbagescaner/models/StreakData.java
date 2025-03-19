package com.example.garbagescaner.models;

import java.util.Calendar;
import java.util.Date;

public class StreakData {
    private int currentStreak;         // Текущее значение ударного режима
    private long lastRecycleDate;      // Дата последней утилизации
    private long lastCheckDate;        // Дата последней проверки состояния

    public StreakData() {
        this.currentStreak = 0;
        this.lastRecycleDate = 0;
        this.lastCheckDate = System.currentTimeMillis();
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public long getLastRecycleDate() {
        return lastRecycleDate;
    }

    public void setLastRecycleDate(long lastRecycleDate) {
        this.lastRecycleDate = lastRecycleDate;
    }

    public long getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(long lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }

    // Получить дату в календарном формате для проверки
    public Calendar getLastRecycleCalendar() {
        Calendar calendar = Calendar.getInstance();
        if (lastRecycleDate > 0) {
            calendar.setTimeInMillis(lastRecycleDate);
        }
        return calendar;
    }

    // Получить текущую дату без времени
    public static Calendar getTodayCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    // Проверить, была ли утилизация сегодня
    public boolean hasRecycledToday() {
        if (lastRecycleDate == 0) {
            return false;
        }

        Calendar lastRecycle = getLastRecycleCalendar();
        Calendar today = getTodayCalendar();

        return lastRecycle.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                lastRecycle.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    // Проверить, была ли утилизация вчера
    public boolean hasRecycledYesterday() {
        if (lastRecycleDate == 0) {
            return false;
        }

        Calendar lastRecycle = getLastRecycleCalendar();
        Calendar yesterday = getTodayCalendar();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        return lastRecycle.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                lastRecycle.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public String toString() {
        return "StreakData{" +
                "currentStreak=" + currentStreak +
                ", lastRecycleDate=" + new Date(lastRecycleDate) +
                ", lastCheckDate=" + new Date(lastCheckDate) +
                '}';
    }
}