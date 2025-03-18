package com.example.garbagescaner.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.R;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.ScanResult;

public class NotRecycledDetailsDialog {
    private final Dialog dialog;
    private final Context context;
    private final OnRecycleClickListener listener;

    public interface OnRecycleClickListener {
        void onRecycleClick(ScanResult scanResult, int position);
    }

    public NotRecycledDetailsDialog(Context context, OnRecycleClickListener listener) {
        this.context = context;
        this.listener = listener;

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_not_recycled_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void show(ScanResult scanResult, int position) {
        // Находим все элементы интерфейса
        TextView tvDetailType = dialog.findViewById(R.id.tvDetailType);
        TextView tvDetailInstructions = dialog.findViewById(R.id.tvDetailInstructions);
        TextView tvDetailCost = dialog.findViewById(R.id.tvDetailCost);
        ImageView ivDetailImage = dialog.findViewById(R.id.ivDetailImage);
        Button btnDialogRecycle = dialog.findViewById(R.id.btnDialogRecycle);

        // Заполняем данными
        tvDetailType.setText("Тип отхода: " + scanResult.getWasteType());
        tvDetailInstructions.setText("Инструкция: " + scanResult.getInstructions());
        tvDetailCost.setText("Примерная стоимость: " + scanResult.getEstimatedCost());

        // Загружаем изображение
        if (scanResult.getImage() != null) {
            Glide.with(context)
                    .load(scanResult.getImage())
                    .into(ivDetailImage);
        }




        btnDialogRecycle.setOnClickListener(v -> {
            // Вызываем метод утилизации через интерфейс
            if (listener != null) {
                listener.onRecycleClick(scanResult, position);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}