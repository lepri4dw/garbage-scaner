// NotificationHelper.java
package com.example.garbagescaner.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.garbagescaner.MainActivity;
import com.example.garbagescaner.R;
import com.example.garbagescaner.models.Achievement;

public class NotificationHelper {
    private static final String CHANNEL_ID = "achievements_channel";
    private static final String CHANNEL_NAME = "Достижения";
    private static final String CHANNEL_DESCRIPTION = "Уведомления о полученных достижениях";
    private static final int NOTIFICATION_ID = 100;

    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Создаем канал уведомлений (требуется для Android 8.0 и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);

            // Регистрируем канал в системе
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showAchievementNotification(Achievement achievement) {
        try {
            // Создаем интент для открытия приложения при нажатии на уведомление
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            // Получаем ID иконки достижения
            int iconResId = getIconResourceId(achievement.getIconResName());

            // Строим уведомление
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_achievement_default)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconResId))
                    .setContentTitle("Достижение разблокировано!")
                    .setContentText(achievement.getTitle())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(achievement.getTitle() + "\n" + achievement.getDescription()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            // Показываем уведомление
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID + achievement.getId(), builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getIconResourceId(String iconName) {
        try {
            return context.getResources().getIdentifier(
                    iconName, "drawable", context.getPackageName());
        } catch (Exception e) {
            return R.drawable.ic_achievement_default;
        }
    }
}