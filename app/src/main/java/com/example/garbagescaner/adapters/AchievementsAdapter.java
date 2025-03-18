package com.example.garbagescaner.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
                // Если иконка не найдена, используем стандартную
                holder.ivIcon.setImageResource(R.drawable.ic_achievement_default);
            }
        } catch (Exception e) {
            holder.ivIcon.setImageResource(R.drawable.ic_achievement_default);
        }

        // Установка состояния разблокировки
        if (achievement.isUnlocked()) {
            holder.tvStatus.setText("Получено: " + formatDate(achievement.getUnlockedTimestamp()));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.tvStatus.setText("Заблокировано");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.itemView.setAlpha(0.6f);

            // Применяем затемнение к иконке для заблокированных достижений
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
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivAchievementIcon);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDescription = itemView.findViewById(R.id.tvAchievementDescription);
            tvStatus = itemView.findViewById(R.id.tvAchievementStatus);
        }
    }
}