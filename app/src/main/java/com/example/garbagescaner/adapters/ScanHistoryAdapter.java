package com.example.garbagescaner.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.R;
import com.example.garbagescaner.models.ScanResult;

import java.util.List;

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<ScanResult> scanResults;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(ScanResult scanResult);
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

        // Загрузка изображения с помощью Glide
        if (scanResult.getImage() != null) {
            Glide.with(context)
                    .load(scanResult.getImage())
                    .placeholder(R.drawable.ic_scanner)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_scanner);
        }

        // Устанавливаем слушатель кликов
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(scanResult);
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewScan);
            tvDate = itemView.findViewById(R.id.tvScanDate);
            tvWasteType = itemView.findViewById(R.id.tvWasteType);
            tvEstimatedCost = itemView.findViewById(R.id.tvEstimatedCost);
        }
    }
}