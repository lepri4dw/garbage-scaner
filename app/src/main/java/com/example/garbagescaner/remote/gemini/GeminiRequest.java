package com.example.garbagescaner.remote.gemini;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeminiRequest {
    @SerializedName("contents")
    private List<Content> contents;

    public GeminiRequest(String text) {
        Part part = new Part(text);
        List<Part> parts = List.of(part);
        Content content = new Content(parts);
        this.contents = List.of(content);
    }

    public static class Content {
        @SerializedName("parts")
        private List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        @SerializedName("text")
        private String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
