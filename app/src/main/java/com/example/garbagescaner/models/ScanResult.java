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

    public ScanResult(long timestamp, String wasteType, String instructions, String estimatedCost, Bitmap image) {
        this.timestamp = timestamp;
        this.wasteType = wasteType;
        this.instructions = instructions;
        this.estimatedCost = estimatedCost;
        this.image = image;
    }

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

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}