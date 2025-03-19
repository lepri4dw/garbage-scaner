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

    // Определение типов отходов для поиска на карте
    private static final String TYPE_PLASTIC = "Пластик";
    private static final String TYPE_GLASS = "Стекло";
    private static final String TYPE_PAPER = "Бумага";
    private static final String TYPE_METAL = "Металл";
    private static final String TYPE_ELECTRONICS = "Электроника";
    private static final String TYPE_FOOD = "Пищевые отходы";
    private static final String TYPE_OTHER = "Прочее"; // Для неопределенных типов

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
        prompt.append(". Выбери ТОЛЬКО ОДИН из следующих типов отходов: Пластик, Стекло, Бумага, Металл, Электроника, Пищевые отходы, Прочее.\n");
        prompt.append("Если тег определяет несколько возможных типов, выбери тот, который преобладает в составе предмета.\n");
        prompt.append("Если не можешь уверенно определить тип или есть сомнения, выбери 'Прочее'.\n");
        prompt.append("Ответь в следующем формате без дополнительных комментариев:\n");
        prompt.append("Тип отхода: [один из указанных типов]\n");
        prompt.append("Инструкция по подготовке: [инструкция]\n");
        prompt.append("Оценочная стоимость: [стоимость]\n");
        prompt.append("Будь максимально точным в определении материала, цвета и типа отхода.\n");
        prompt.append("Для стоимости используй актуальные цены для Бишкека на 2025 год, указывай в сомах за единицу измерения (например: 10-15 сом/кг).\n");
        prompt.append("Если точной информации о цене нет, укажи примерный диапазон на основе имеющихся данных.\n");
        prompt.append("Если совсем нет информации о цене, укажи 'Информация о стоимости недоступна'.");
        prompt.append("Для электроники всегда пиши, что нет информации о цене, укажи 'Информация о стоимости недоступна'.");

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
                        Log.e(TAG, "Ошибка при обработке ответа Gemini: " + e.getMessage());
                        // В случае ошибки создаем объект с типом "Прочее"
                        listener.onSuccess(new GarbageInfo(TYPE_OTHER,
                                "Информация недоступна", "Информация недоступна"));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Ошибка API: " + errorBody);
                        // При ошибке API также возвращаем объект с типом "Прочее" вместо ошибки
                        listener.onSuccess(new GarbageInfo(TYPE_OTHER,
                                "Информация недоступна", "Информация недоступна"));
                    } catch (IOException e) {
                        // При ошибке также возвращаем объект с типом "Прочее"
                        listener.onSuccess(new GarbageInfo(TYPE_OTHER,
                                "Информация недоступна", "Информация недоступна"));
                    }
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e(TAG, "Ошибка соединения: " + t.getMessage());
                // При ошибке соединения также возвращаем объект с типом "Прочее"
                listener.onSuccess(new GarbageInfo(TYPE_OTHER,
                        "Информация недоступна", "Информация недоступна"));
            }

            private GarbageInfo parseGeminiResponse(String text) {
                String rawType = "";
                String instructions = "";
                String estimatedCost = "";

                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Тип отхода:")) {
                        rawType = line.substring("Тип отхода:".length()).trim();
                    } else if (line.startsWith("Инструкция по подготовке:")) {
                        instructions = line.substring("Инструкция по подготовке:".length()).trim();
                    } else if (line.startsWith("Оценочная стоимость:")) {
                        estimatedCost = line.substring("Оценочная стоимость:".length()).trim();
                    }
                }

                // Проверка на пустое значение типа
                if (rawType.isEmpty()) {
                    rawType = TYPE_OTHER;
                }

                // Определяем стандартизированный тип отхода на основе ответа Gemini
                String standardizedType = standardizeWasteType(rawType);

                return new GarbageInfo(standardizedType, instructions, estimatedCost);
            }

            // Метод для стандартизации типа отхода
            private String standardizeWasteType(String rawType) {
                rawType = rawType.toLowerCase();

                if (rawType.contains("пластик") || rawType.contains("пэт")) {
                    return TYPE_PLASTIC;
                } else if (rawType.contains("стекл")) {
                    return TYPE_GLASS;
                } else if (rawType.contains("бумаг") || rawType.contains("картон")) {
                    return TYPE_PAPER;
                } else if (rawType.contains("метал")) {
                    return TYPE_METAL;
                } else if (rawType.contains("электро") || rawType.contains("техник")) {
                    return TYPE_ELECTRONICS;
                } else if (rawType.contains("пищев") || rawType.contains("еда") || rawType.contains("пища") ||
                        rawType.contains("органич") || rawType.contains("компост") || rawType.contains("био")) {
                    return TYPE_FOOD;
                } else {
                    return TYPE_OTHER;
                }
            }
        });
    }
}