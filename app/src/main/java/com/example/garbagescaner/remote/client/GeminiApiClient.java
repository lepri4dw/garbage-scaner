package com.example.garbagescaner.remote.client;

import android.util.Log;

import com.example.garbagescaner.remote.gemini.GarbageInfo;
import com.example.garbagescaner.remote.gemini.GeminiRequest;
import com.example.garbagescaner.remote.gemini.GeminiResponse;
import com.example.garbagescaner.remote.gemini.GeminiService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiApiClient {
    private static final String TAG = "GeminiApiClient";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private final GeminiService service;
    private final String apiKey;

    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public interface GeminiApiListener {
        void onSuccess(GarbageInfo garbageInfo);

        void onError(Exception e);
    }

    public void getGarbageInfo(List<String> labels, GeminiApiListener listener) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Определи тип отхода на основе следующих тегов: ");
        prompt.append(String.join(", ", labels));
        prompt.append(". Ответь в следующем формате без дополнительных комментариев:\n");
        prompt.append("Тип отхода: [тип отхода]\n");
        prompt.append("Инструкция по подготовке: [инструкция]\n");
        prompt.append("Оценочная стоимость: [стоимость]\n");
        prompt.append("Определи тип отхода на основе следующих тегов: ");
        prompt.append(String.join(", ", labels));
        prompt.append(". Будь максимально точным в определении материала, цвета и типа отхода.");
        prompt.append(". Назови приблизительную цену в сомах для Бишкека в 2025 году. То есть в цене лучше писать например 10 сом/кг");
        prompt.append("Ответь в следующем формате без дополнительных комментариев:\n");

        GeminiRequest request = new GeminiRequest(prompt.toString());

        service.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String text = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                        GarbageInfo garbageInfo = parseGeminiResponse(text);
                        listener.onSuccess(garbageInfo);
                    } catch (Exception e) {
                        listener.onError(new Exception("Ошибка при обработке ответа Gemini"));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        listener.onError(new Exception("Ошибка API: " + errorBody));
                    } catch (IOException e) {
                        listener.onError(new Exception("Ошибка при чтении ответа API"));
                    }
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                listener.onError(new Exception("Ошибка соединения: " + t.getMessage()));
            }

            private GarbageInfo parseGeminiResponse(String text) {
                String type = "";
                String instructions = "";
                String estimatedCost = "";

                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Тип отхода:")) {
                        type = line.substring("Тип отхода:".length()).trim();
                    } else if (line.startsWith("Инструкция по подготовке:")) {
                        instructions = line.substring("Инструкция по подготовке:".length()).trim();
                    } else if (line.startsWith("Оценочная стоимость:")) {
                        estimatedCost = line.substring("Оценочная стоимость:".length()).trim();
                    }
                }

                return new GarbageInfo(type, instructions, estimatedCost);
            }
        });
    }
}
