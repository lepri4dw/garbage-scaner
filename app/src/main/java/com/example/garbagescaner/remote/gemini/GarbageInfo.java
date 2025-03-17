package com.example.garbagescaner.remote.gemini;

public class GarbageInfo {
    private String type;
    private String instructions;
    private String estimatedCost;

    public GarbageInfo(String type, String instructions, String estimatedCost) {
        this.type = type;
        this.instructions = instructions;
        this.estimatedCost = estimatedCost;
    }

    public String getType() {
        return type;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getEstimatedCost() {
        return estimatedCost;
    }
}