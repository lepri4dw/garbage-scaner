package com.example.garbagescaner;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.remote.client.GeminiApiClient;
import com.example.garbagescaner.remote.client.VisionApiClient;
import com.example.garbagescaner.remote.gemini.GarbageInfo;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String GEMINI_API_KEY = "AIzaSyCl8qzbbpDJ-ltNr_86SSFMTnbC6vhXvCk";

    private ImageView imageView;
    private Button btnSelectImage;
    private Button btnScanWithCamera; // Кнопка для сканирования камерой
    private Button btnFindRecyclingPoints; // Кнопка для поиска пунктов приема
    private TextView tvLoading;
    private CardView cardResult;
    private TextView tvGarbageType;
    private TextView tvInstructions;
    private TextView tvEstimatedCost;

    private VisionApiClient visionApiClient;
    private GeminiApiClient geminiApiClient;

    // Лаунчеры для выбора изображения и сканирования камерой
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private String currentWasteType; // Для хранения текущего типа отхода

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeClients();
        setupLaunchers();
        setupListeners();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnScanWithCamera = findViewById(R.id.btnScanWithCamera);
        btnFindRecyclingPoints = findViewById(R.id.btnFindRecyclingPoints);
        tvLoading = findViewById(R.id.tvLoading);
        cardResult = findViewById(R.id.cardResult);
        tvGarbageType = findViewById(R.id.tvGarbageType);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost);
    }

    private void initializeClients() {
        visionApiClient = new VisionApiClient(this);
        geminiApiClient = new GeminiApiClient(GEMINI_API_KEY);
    }

    private void setupLaunchers() {
        // Настройка лаунчера для выбора изображения из галереи
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                displayImage(bitmap);
                                processImage(bitmap);
                            } catch (IOException e) {
                                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Настройка лаунчера для получения изображения с камеры
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String imagePath = result.getData().getStringExtra("image_path");
                        if (imagePath != null) {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                                displayImage(bitmap);
                                processImage(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(this, "Ошибка загрузки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnScanWithCamera.setOnClickListener(v -> openCameraScanner());
        btnFindRecyclingPoints.setOnClickListener(v -> openMap());
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        Dexter.withContext(this)
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        openImagePicker();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Необходимо разрешение для выбора изображения", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // Метод для запуска активности сканера
    private void openCameraScanner() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    // Новый метод для открытия карты с пунктами приема
    private void openMap() {
        Intent intent = new Intent(this, MapActivity.class);
        // Передаем тип отхода, чтобы сразу показать нужные пункты приема
        if (currentWasteType != null && !currentWasteType.isEmpty()) {
            intent.putExtra("waste_type", currentWasteType);
        }
        startActivity(intent);
    }

    private void displayImage(Bitmap bitmap) {
        Glide.with(this).load(bitmap).into(imageView);
    }

    private void processImage(Bitmap bitmap) {
        showLoading(true);
        hideResult();

        visionApiClient.analyzeImage(bitmap, new VisionApiClient.VisionApiListener() {
            @Override
            public void onSuccess(List<String> labels) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Обнаружены метки: " + String.join(", ", labels), Toast.LENGTH_SHORT).show();
                    getGarbageInfo(labels);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Ошибка анализа изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void getGarbageInfo(List<String> labels) {
        geminiApiClient.getGarbageInfo(labels, new GeminiApiClient.GeminiApiListener() {
            @Override
            public void onSuccess(GarbageInfo garbageInfo) {
                runOnUiThread(() -> {
                    showLoading(false);
                    displayResult(garbageInfo);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(MainActivity.this, "Ошибка получения информации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        tvLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSelectImage.setEnabled(!isLoading);
        btnScanWithCamera.setEnabled(!isLoading);
        btnFindRecyclingPoints.setEnabled(!isLoading);
    }

    private void hideResult() {
        cardResult.setVisibility(View.GONE);
        btnFindRecyclingPoints.setVisibility(View.GONE);
    }

    private void displayResult(GarbageInfo garbageInfo) {
        tvGarbageType.setText("Тип отхода: " + garbageInfo.getType());
        tvInstructions.setText("Инструкция по подготовке: " + garbageInfo.getInstructions());
        tvEstimatedCost.setText("Оценочная стоимость: " + garbageInfo.getEstimatedCost());
        cardResult.setVisibility(View.VISIBLE);

        // Показываем кнопку поиска пунктов приема
        btnFindRecyclingPoints.setVisibility(View.VISIBLE);

        // Сохраняем текущий тип отхода для передачи в MapActivity
        currentWasteType = garbageInfo.getType();
    }
}