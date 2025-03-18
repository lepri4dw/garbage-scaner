package com.example.garbagescaner.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.R;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.ScanResult;

public class RecyclingDetailsDialog {
    private Dialog dialog;
    private Context context;
    private TextView tvTitle;
    private TextView tvInstructions;
    private TextView tvCost;
    private TextView tvDate;
    private ImageView imageView;
    private ImageView mapButton;
    private Button btnClose;
    private View contentView;

    public RecyclingDetailsDialog(Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recycling_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Инициализация элементов
        tvTitle = dialog.findViewById(R.id.tvDetailTitle);
        tvInstructions = dialog.findViewById(R.id.tvDetailInstructions);
        tvCost = dialog.findViewById(R.id.tvDetailCost);
        tvDate = dialog.findViewById(R.id.tvDetailDate);
        imageView = dialog.findViewById(R.id.ivDetailImage);
        mapButton = dialog.findViewById(R.id.ivMapIcon);
        btnClose = dialog.findViewById(R.id.btnClose);
        contentView = dialog.findViewById(R.id.dialogContentView);

        btnClose.setOnClickListener(v -> dialog.dismiss());
    }

    public void show(ScanResult scanResult) {
        if (scanResult == null) return;

        // Заполняем данные
        tvTitle.setText("Утилизированный: " + scanResult.getWasteType());
        tvInstructions.setText(scanResult.getInstructions());
        tvCost.setText("Примерная стоимость: " + scanResult.getEstimatedCost());
        tvDate.setText("Дата утилизации: " + scanResult.getFormattedRecycledDate());

        // Загружаем изображение
        if (scanResult.getImage() != null) {
            Glide.with(context).load(scanResult.getImage()).into(imageView);
        }

        // Настраиваем кнопку карты
        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapActivity.class);
            intent.putExtra("waste_type", scanResult.getWasteType());
            context.startActivity(intent);
            dialog.dismiss();
        });

        // Запускаем анимацию появления
        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        contentView.startAnimation(fadeInAnimation);

        dialog.show();
    }
}