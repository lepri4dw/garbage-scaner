package com.example.garbagescaner.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.garbagescaner.R;
import com.example.garbagescaner.models.Achievement;

public class AchievementUnlockDialog {
    private static final String TAG = "AchievementDialog";
    private final Context context;
    private Dialog dialog;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public AchievementUnlockDialog(Context context) {
        this.context = context;
        prepareDialog();
    }

    private void prepareDialog() {
        try {
            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_achievement_unlock, null);
            dialog.setContentView(view);

            // Настройка окна диалога
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.TOP);

                // Устанавливаем анимацию для окна
                WindowManager.LayoutParams params = window.getAttributes();
                params.y = 50; // смещение сверху
                window.setAttributes(params);

                // Делаем диалог некликабельным и не отменяемым по нажатию вне его
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
            }

            // Инициализируем проигрыватель
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.achievement_unlock);
                if (mediaPlayer == null) {
                    Log.e(TAG, "Failed to create MediaPlayer. Sound file may be missing or corrupted.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing MediaPlayer: " + e.getMessage());
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing dialog: " + e.getMessage());
        }
    }

    public void show(Achievement achievement) {
        try {
            if (achievement == null || dialog == null) {
                Log.e(TAG, "Cannot show dialog: achievement or dialog is null");
                return;
            }

            // Находим все нужные View
            ImageView iconView = dialog.findViewById(R.id.ivAchievementUnlockIcon);
            TextView titleView = dialog.findViewById(R.id.tvAchievementUnlockTitle);
            TextView descriptionView = dialog.findViewById(R.id.tvAchievementUnlockDescription);
            View contentView = dialog.findViewById(R.id.achievementDialogContent);

            if (iconView == null || titleView == null || descriptionView == null || contentView == null) {
                Log.e(TAG, "One or more Views not found in dialog layout");
                return;
            }

            // Устанавливаем данные
            titleView.setText(achievement.getTitle());
            descriptionView.setText(achievement.getDescription());

            // Устанавливаем иконку
            try {
                Resources resources = context.getResources();
                int iconId = resources.getIdentifier(
                        achievement.getIconResName(), "drawable", context.getPackageName());

                if (iconId != 0) {
                    iconView.setImageResource(iconId);
                } else {
                    iconView.setImageResource(R.drawable.ic_achievement_default);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting icon: " + e.getMessage());
                iconView.setImageResource(R.drawable.ic_achievement_default);
            }

            // Загружаем анимацию появления
            Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.achievement_slide_in);
            contentView.startAnimation(slideIn);

            // Воспроизводим звук
            if (mediaPlayer != null) {
                try {
                    // Сбрасываем, если уже проигрывался
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer = MediaPlayer.create(context, R.raw.achievement_unlock);
                    }
                    mediaPlayer.start();
                } catch (Exception e) {
                    Log.e(TAG, "Error playing sound: " + e.getMessage());
                }
            }

            // Показываем диалог
            dialog.show();
            Log.d(TAG, "Achievement dialog shown for: " + achievement.getTitle());

            // Настраиваем автоматическое закрытие через 5 секунд
            handler.removeCallbacksAndMessages(null); // Убираем предыдущие колбэки
            handler.postDelayed(this::dismiss, 5000);
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage());
        }
    }

    private void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                // Загружаем анимацию исчезновения
                View contentView = dialog.findViewById(R.id.achievementDialogContent);
                if (contentView != null) {
                    Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.achievement_slide_out);

                    slideOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            dialog.dismiss();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });

                    contentView.startAnimation(slideOut);
                } else {
                    dialog.dismiss();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
        }
    }

    public void release() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            handler.removeCallbacksAndMessages(null);

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing resources: " + e.getMessage());
        }
    }
}