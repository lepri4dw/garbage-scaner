package com.example.garbagescaner.database;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.example.garbagescaner.R;
import com.example.garbagescaner.models.ScanResult;
import com.example.garbagescaner.models.StatisticPeriod;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanHistoryManager {
    private static final String TAG = "ScanHistoryManager";
    private static final String PREF_NAME = "scan_history_prefs";
    private static final String KEY_HISTORY = "scan_history";
    private static final int MAX_HISTORY_SIZE = 50; // Максимальное количество элементов в истории

    private final SharedPreferences preferences;
    private final Gson gson;
    private final Context context;
    private final AchievementManager achievementManager;

    public ScanHistoryManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Настраиваем Gson для сериализации/десериализации изображений в формате Base64
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Bitmap.class, new BitmapSerializer());
        gsonBuilder.registerTypeAdapter(Bitmap.class, new BitmapDeserializer());
        gson = gsonBuilder.create();

        // Инициализируем менеджер достижений
        achievementManager = new AchievementManager(context);
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

        // Проверяем достижения
        achievementManager.checkAndUnlockAchievements(history);
    }
    private void showStreakMilestoneDialog(int streakDays) {
        // Создаем и настраиваем диалог
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Поздравляем!");

        String message;
        switch (streakDays) {
            case 7:
                message = "Вы утилизируете отходы 7 дней подряд! Продолжайте в том же духе!";
                break;
            case 30:
                message = "Целый месяц ежедневных утилизаций! Вы настоящий защитник природы!";
                break;
            case 100:
                message = "100 дней подряд! Ваш вклад в защиту окружающей среды неоценим!";
                break;
            default:
                message = "Вы утилизируете отходы " + streakDays + " дней подряд!";
                break;
        }

        builder.setMessage(message);
        builder.setPositiveButton("Спасибо!", null);
        builder.setIcon(R.drawable.ic_streak_flame);

        // Показываем диалог
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Метод для обновления результата сканирования (маркировка как утилизированного)
    public void markAsRecycled(int position) {
        List<ScanResult> history = getAllScanResults();

        if (position >= 0 && position < history.size()) {
            ScanResult result = history.get(position);

            if (!result.isRecycled()) {
                Log.d(TAG, "Marking scan as recycled: " + result.getWasteType());

                // Маркируем как утилизированный
                result.markAsRecycled();

                // Сохраняем историю
                saveHistory(history);
                StreakManager streakManager = new StreakManager(context);
                boolean streakIncreased = streakManager.registerRecycling();

                if (streakIncreased) {
                    int currentStreak = streakManager.getCurrentStreak();
                    Toast.makeText(context,
                            "Ударный режим: " + currentStreak + " дней подряд!",
                            Toast.LENGTH_SHORT).show();

                    // Можно добавить специальное уведомление для круглых чисел
                    if (currentStreak == 7 || currentStreak == 30 || currentStreak == 100) {
                        showStreakMilestoneDialog(currentStreak);
                    }
                }

                // Проверяем достижения
                AchievementManager achievementManager = new AchievementManager(context);
                achievementManager.checkAndUnlockAchievements(history);
            }
        }
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

    // Получение статистики по утилизированным отходам
    public Map<String, Double> getRecyclingStatistics(StatisticPeriod period) {
        List<ScanResult> history = getAllScanResults();
        Map<String, Double> statistics = new HashMap<>();

        // Инициализируем каждый тип отхода нулем
        statistics.put("Пластик", 0.0);
        statistics.put("Стекло", 0.0);
        statistics.put("Бумага", 0.0);
        statistics.put("Металл", 0.0);
        statistics.put("Электроника", 0.0);
        statistics.put("Пищевые отходы", 0.0);
        statistics.put("Прочее", 0.0);

        // Получаем временную метку начала периода
        long periodStartTime = getPeriodStartTimestamp(period);

        for (ScanResult result : history) {
            // Учитываем только утилизированные отходы в заданном периоде
            if (result.isRecycled() && result.getRecycledTimestamp() >= periodStartTime) {
                String wasteType = result.getWasteType();
                double cost = result.getNumericCost();

                // Определяем категорию отхода
                String category = "Прочее";
                if (wasteType.contains("Пластик")) {
                    category = "Пластик";
                } else if (wasteType.contains("Стекло")) {
                    category = "Стекло";
                } else if (wasteType.contains("Бумага")) {
                    category = "Бумага";
                } else if (wasteType.contains("Металл")) {
                    category = "Металл";
                } else if (wasteType.contains("Электроника")) {
                    category = "Электроника";
                } else if (wasteType.contains("Пищевые")) {
                    category = "Пищевые отходы";
                }

                // Добавляем стоимость к соответствующей категории
                statistics.put(category, statistics.get(category) + cost);
            }
        }

        return statistics;
    }

    // Получение общего количества утилизированных отходов по периодам
    public int getTotalRecycledCount(StatisticPeriod period) {
        List<ScanResult> history = getAllScanResults();
        int count = 0;

        // Получаем временную метку начала периода
        long periodStartTime = getPeriodStartTimestamp(period);

        for (ScanResult result : history) {
            if (result.isRecycled() && result.getRecycledTimestamp() >= periodStartTime) {
                count++;
            }
        }

        return count;
    }

    // Рассчет временной метки начала периода
    private long getPeriodStartTimestamp(StatisticPeriod period) {
        Calendar calendar = Calendar.getInstance();

        switch (period) {
            case TODAY:
                // Начало текущего дня
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;

            case WEEK:
                // Начало текущей недели (понедельник)
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                break;

            case MONTH:
                // Начало текущего месяца
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;

            case ALL_TIME:
            default:
                // Все время - возвращаем 0
                return 0;
        }

        return calendar.getTimeInMillis();
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




    public Map<String, Integer> getRecycledCountByType(StatisticPeriod period) {
        List<ScanResult> history = getAllScanResults();
        Map<String, Integer> countByType = new HashMap<>();

        // Инициализируем все типы нулями
        countByType.put("Пластик", 0);
        countByType.put("Стекло", 0);
        countByType.put("Бумага", 0);
        countByType.put("Металл", 0);
        countByType.put("Электроника", 0);
        countByType.put("Пищевые отходы", 0);
        countByType.put("Прочее", 0);

        // Получаем временную метку начала периода
        long periodStartTime = getPeriodStartTimestamp(period);

        for (ScanResult result : history) {
            // Учитываем только утилизированные отходы в заданном периоде
            if (result.isRecycled() && result.getRecycledTimestamp() >= periodStartTime) {
                String wasteType = result.getWasteType();

                // Определяем категорию отхода
                String category = getWasteCategory(wasteType);

                // Увеличиваем счетчик для этой категории
                countByType.put(category, countByType.get(category) + 1);
            }
        }

        return countByType;
    }

    // Вспомогательный метод для определения категории отхода
    private String getWasteCategory(String wasteType) {
        wasteType = wasteType.toLowerCase();

        if (wasteType.contains("пластик")) {
            return "Пластик";
        } else if (wasteType.contains("стекл")) {
            return "Стекло";
        } else if (wasteType.contains("бумаг") || wasteType.contains("картон")) {
            return "Бумага";
        } else if (wasteType.contains("метал")) {
            return "Металл";
        } else if (wasteType.contains("электрон")) {
            return "Электроника";
        } else if (wasteType.contains("пищев") || wasteType.contains("орган")) {
            return "Пищевые отходы";
        } else {
            return "Прочее";
        }
    }
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