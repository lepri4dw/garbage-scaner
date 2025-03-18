package com.example.garbagescaner.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.R;
import com.example.garbagescaner.dialogs.RecyclingDetailsDialog;
import com.example.garbagescaner.models.ScanResult;

import java.util.List;

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {
    private static final String TAG = "ScanHistoryAdapter";
    private final Context context;
    private final List<ScanResult> scanResults;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(ScanResult scanResult);
        void onRecycleClick(ScanResult scanResult, int position);
    }

    public ScanHistoryAdapter(Context context, List<ScanResult> scanResults, OnHistoryItemClickListener listener) {
        this.context = context;
        this.scanResults = scanResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult scanResult = scanResults.get(position);

        holder.tvDate.setText(scanResult.getFormattedDate());
        holder.tvWasteType.setText(scanResult.getWasteType());
        holder.tvEstimatedCost.setText(scanResult.getEstimatedCost());

        // Устанавливаем статус утилизации
        if (scanResult.isRecycled()) {
            holder.btnRecycle.setVisibility(View.GONE);
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("Утилизировано: " + scanResult.getFormattedRecycledDate());
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
            // Добавляем затемнение для утилизированных элементов
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.btnRecycle.setVisibility(View.VISIBLE);
            holder.tvStatus.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
        }

        // Загрузка изображения с помощью Glide
        if (scanResult.getImage() != null) {
            Glide.with(context)
                    .load(scanResult.getImage())
                    .placeholder(R.drawable.ic_scanner)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_scanner);
        }

        // Устанавливаем слушатель клика по элементу
        holder.itemView.setOnClickListener(v -> {
            if (scanResult.isRecycled()) {
                // Для утилизированных элементов показываем диалог с деталями
                RecyclingDetailsDialog dialog = new RecyclingDetailsDialog(context);
                dialog.show(scanResult);
            } else if (listener != null) {
                // Для неутилизированных элементов открываем карту
                listener.onItemClick(scanResult);
            }
        });

        // Устанавливаем слушатель для кнопки утилизации
        final int itemPosition = position;
        holder.btnRecycle.setOnClickListener(v -> {
            Log.d(TAG, "Recycle button clicked for position: " + itemPosition);
            if (listener != null) {
                listener.onRecycleClick(scanResult, itemPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvDate;
        TextView tvWasteType;
        TextView tvEstimatedCost;
        TextView tvStatus;
        Button btnRecycle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewScan);
            tvDate = itemView.findViewById(R.id.tvScanDate);
            tvWasteType = itemView.findViewById(R.id.tvWasteType);
            tvEstimatedCost = itemView.findViewById(R.id.tvEstimatedCost);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnRecycle = itemView.findViewById(R.id.btnRecycle);
        }
    }
}