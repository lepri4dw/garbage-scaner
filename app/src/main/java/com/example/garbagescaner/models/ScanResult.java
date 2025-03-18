package com.example.garbagescaner.models;

import android.graphics.Bitmap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScanResult {
    private long timestamp;
    private String wasteType;
    private String instructions;
    private String estimatedCost;
    private Bitmap image;
    private boolean recycled; // Флаг утилизации
    private long recycledTimestamp; // Время утилизации

    public ScanResult(long timestamp, String wasteType, String instructions, String estimatedCost, Bitmap image) {
        this.timestamp = timestamp;
        this.wasteType = wasteType;
        this.instructions = instructions;
        this.estimatedCost = estimatedCost;
        this.image = image;
        this.recycled = false;
        this.recycledTimestamp = 0;
    }

    // Геттеры
    public long getTimestamp() {
        return timestamp;
    }

    public String getWasteType() {
        return wasteType;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getEstimatedCost() {
        return estimatedCost;
    }

    public Bitmap getImage() {
        return image;
    }

    public boolean isRecycled() {
        return recycled;
    }

    public long getRecycledTimestamp() {
        return recycledTimestamp;
    }

    // Маркировка как утилизированного
    public void markAsRecycled() {
        this.recycled = true;
        this.recycledTimestamp = System.currentTimeMillis();
    }

    // Получение форматированной даты сканирования
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Получение форматированной даты утилизации
    public String getFormattedRecycledDate() {
        if (!recycled) return "Не утилизировано";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(recycledTimestamp));
    }

    // Извлечение числового значения стоимости (для статистики)
    public double getNumericCost() {
        try {
            // Пытаемся извлечь первое число из строки
            String costStr = estimatedCost.replaceAll("[^\\d.,]", " ")
                    .trim().split("\\s+")[0]
                    .replace(',', '.');
            return Double.parseDouble(costStr);
        } catch (Exception e) {
            return 0.0;
        }
    }
}