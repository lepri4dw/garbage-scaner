package com.example.garbagescaner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.example.garbagescaner.models.ScanResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScanHistoryManager {
    private static final String TAG = "ScanHistoryManager";
    private static final String PREF_NAME = "scan_history_prefs";
    private static final String KEY_HISTORY = "scan_history";
    private static final int MAX_HISTORY_SIZE = 50; // Максимальное количество элементов в истории

    private final SharedPreferences preferences;
    private final Gson gson;

    public ScanHistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Настраиваем Gson для сериализации/десериализации изображений в формате Base64
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Bitmap.class, new BitmapSerializer());
        gsonBuilder.registerTypeAdapter(Bitmap.class, new BitmapDeserializer());
        gson = gsonBuilder.create();
    }

    public List<ScanResult> getAllScanResults() {
        String historyJson = preferences.getString(KEY_HISTORY, null);
        if (historyJson == null) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<ScanResult>>() {}.getType();
            List<ScanResult> history = gson.fromJson(historyJson, type);

            // Сортируем по дате (сначала новые)
            Collections.sort(history, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

            return history;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении истории: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void addScanResult(ScanResult scanResult) {
        List<ScanResult> history = getAllScanResults();

        // Добавляем новый результат
        history.add(scanResult);

        // Если история превышает максимальный размер, удаляем старые записи
        if (history.size() > MAX_HISTORY_SIZE) {
            // Сортируем по дате (сначала новые)
            Collections.sort(history, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

            // Оставляем только MAX_HISTORY_SIZE записей
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        // Сохраняем обновленную историю
        saveHistory(history);
    }

    private void saveHistory(List<ScanResult> history) {
        try {
            String historyJson = gson.toJson(history);
            preferences.edit().putString(KEY_HISTORY, historyJson).apply();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении истории: " + e.getMessage());
        }
    }

    public void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply();
    }

    /**
     * Сериализатор для преобразования Bitmap в строку Base64
     */
    private static class BitmapSerializer implements JsonSerializer<Bitmap> {
        @Override
        public JsonElement serialize(Bitmap src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src != null) {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    // Сжимаем изображение для экономии места
                    src.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                    jsonObject.addProperty("data", base64);
                    jsonObject.addProperty("width", src.getWidth());
                    jsonObject.addProperty("height", src.getHeight());
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при сериализации изображения: " + e.getMessage());
                }
            }
            return jsonObject;
        }
    }

    /**
     * Десериализатор для преобразования строки Base64 в Bitmap
     */
    private static class BitmapDeserializer implements JsonDeserializer<Bitmap> {
        @Override
        public Bitmap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("data")) {
                try {
                    String base64 = jsonObject.get("data").getAsString();
                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при десериализации изображения: " + e.getMessage());
                }
            }
            return null;
        }
    }
}