package com.example.garbagescaner.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.garbagescaner.R;
import com.example.garbagescaner.models.Achievement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {

    private final Context context;
    private final List<Achievement> achievements;

    public AchievementsAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    // В методе onBindViewHolder в AchievementsAdapter
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);

        holder.tvTitle.setText(achievement.getTitle());
        holder.tvDescription.setText(achievement.getDescription());

        // Установка иконки достижения
        try {
            Resources resources = context.getResources();
            int iconId = resources.getIdentifier(
                    achievement.getIconResName(), "drawable", context.getPackageName());

            if (iconId != 0) {
                holder.ivIcon.setImageResource(iconId);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_achievement_default);
            }
        } catch (Exception e) {
            Log.e("AchievementsAdapter", "Error loading icon: " + e.getMessage());
            holder.ivIcon.setImageResource(R.drawable.ic_achievement_default);
        }

        // Расчет и отображение прогресса
        int progress = achievement.getProgressPercentage();
        holder.progressBar.setMax(100); // Убедимся, что максимум установлен правильно
        holder.progressBar.setProgress(progress);

        Log.d("AchievementsAdapter", "Achievement: " + achievement.getTitle() +
                ", Progress: " + achievement.getCurrentValue() + "/" + achievement.getTargetValue() +
                " (" + progress + "%)");

        // Отображение статуса
        if (achievement.isUnlocked()) {
            holder.tvStatus.setText("Получено");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
            holder.progressBar.setProgress(100); // Всегда 100% для разблокированных
            holder.itemView.setAlpha(1.0f);
            // Убираем затемнение для разблокированных достижений
            holder.ivIcon.clearColorFilter();
        } else {
            holder.tvStatus.setText(String.format("Прогресс: %d из %d",
                    achievement.getCurrentValue(), achievement.getTargetValue()));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.itemView.setAlpha(0.8f);
            // Затемняем иконку для не разблокированных достижений
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_hint));
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvDescription;
        ProgressBar progressBar;
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivAchievementIcon);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDescription = itemView.findViewById(R.id.tvAchievementDescription);
            progressBar = itemView.findViewById(R.id.progressBarAchievement);
            tvStatus = itemView.findViewById(R.id.tvAchievementStatus);
        }
    }
}