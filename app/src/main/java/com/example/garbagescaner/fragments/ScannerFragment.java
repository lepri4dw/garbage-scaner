package com.example.garbagescaner.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.garbagescaner.CameraActivity;
import com.example.garbagescaner.MainActivity;
import com.example.garbagescaner.R;
import com.example.garbagescaner.database.ScanHistoryManager;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.ScanResult;
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

public class ScannerFragment extends Fragment {

    private static final String GEMINI_API_KEY = "AIzaSyCl8qzbbpDJ-ltNr_86SSFMTnbC6vhXvCk";
    private static final String TAG = "ScannerFragment";

    private ImageView imageView;
    private Button btnSelectImage;
    private Button btnScanWithCamera;
    private Button btnFindRecyclingPoints;
    private TextView tvLoading;
    private CardView cardResult;
    private TextView tvGarbageType;
    private TextView tvInstructions;
    private TextView tvEstimatedCost;
    private ProgressBar progressBar;
    private TextView tvPlaceholder;

    private VisionApiClient visionApiClient;
    private GeminiApiClient geminiApiClient;
    private ScanHistoryManager historyManager;

    private boolean isProcessing = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private String currentWasteType; // Для хранения текущего типа отхода
    private String currentInstructions;
    private String currentEstimatedCost;
    private Bitmap currentImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeClients();
        setupLaunchers();
        setupListeners();

        setupButtonAnimations();

        // Инициализируем менеджер истории сканирований
        historyManager = new ScanHistoryManager(requireContext());

        // Обработчик нажатия кнопки "Назад"
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isProcessing) {
                            // Если идет обработка, показываем диалог подтверждения
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                            builder.setTitle("Прервать анализ?");
                            builder.setMessage("Анализ изображения еще не завершен. Вы уверены, что хотите прервать процесс?");
                            builder.setPositiveButton("Да", (dialog, which) -> {
                                // Прерываем процесс и возвращаемся назад
                                isProcessing = false;
                                showLoading(false);

                                // Включаем навигацию обратно
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).setNavigationEnabled(true);
                                }

                                // Разрешаем стандартную обработку нажатия "Назад"
                                this.setEnabled(false);
                                requireActivity().onBackPressed();
                            });
                            builder.setNegativeButton("Нет", null);
                            builder.show();
                        } else {
                            // Если обработка не идет, разрешаем стандартную обработку нажатия "Назад"
                            this.setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                });
    }

    private void setupButtonAnimations() {
        // Загружаем анимацию пульсации
        Animation pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale);

        // Добавляем слушатели касания для анимации кнопок
        btnSelectImage.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation);
            }
            return false;
        });

        btnScanWithCamera.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation);
            }
            return false;
        });

        btnFindRecyclingPoints.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation);
            }
            return false;
        });
    }

    private void initializeViews(View view) {
        imageView = view.findViewById(R.id.imageView);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        btnScanWithCamera = view.findViewById(R.id.btnScanWithCamera);
        btnFindRecyclingPoints = view.findViewById(R.id.btnFindRecyclingPoints);
        tvLoading = view.findViewById(R.id.tvLoading);
        cardResult = view.findViewById(R.id.cardResult);
        tvGarbageType = view.findViewById(R.id.tvGarbageType);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        tvEstimatedCost = view.findViewById(R.id.tvEstimatedCost);
        progressBar = view.findViewById(R.id.progressBar);
        tvPlaceholder = view.findViewById(R.id.tvPlaceholder);
    }

    private void initializeClients() {
        visionApiClient = new VisionApiClient(requireContext());
        geminiApiClient = new GeminiApiClient(GEMINI_API_KEY);
    }

    private void setupLaunchers() {
        // Настройка лаунчера для выбора изображения из галереи
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == -1 && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                                currentImage = bitmap;
                                displayImage(bitmap);
                                processImage(bitmap);
                            } catch (IOException e) {
                                Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Настройка лаунчера для получения изображения с камеры
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == -1 && result.getData() != null) {
                        String imagePath = result.getData().getStringExtra("image_path");
                        if (imagePath != null) {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                                currentImage = bitmap;
                                displayImage(bitmap);
                                processImage(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Ошибка загрузки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        Dexter.withContext(requireContext())
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        openImagePicker();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(requireContext(), "Необходимо разрешение для выбора изображения", Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(requireContext(), CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    // Метод для открытия карты с пунктами приема
    private void openMap() {
        Intent intent = new Intent(requireContext(), MapActivity.class);
        // Передаем тип отхода, чтобы сразу показать нужные пункты приема
        if (currentWasteType != null && !currentWasteType.isEmpty()) {
            intent.putExtra("waste_type", currentWasteType);
        }
        startActivity(intent);
    }

    private void displayImage(Bitmap bitmap) {
        tvPlaceholder.setVisibility(View.GONE);
        Glide.with(this).load(bitmap).into(imageView);
    }

    private void processImage(Bitmap bitmap) {
        isProcessing = true;
        showLoading(true);
        hideResult();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationEnabled(false);
        }

        visionApiClient.analyzeImage(bitmap, new VisionApiClient.VisionApiListener() {
            @Override
            public void onSuccess(List<String> labels) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Обнаружены метки: " + String.join(", ", labels), Toast.LENGTH_SHORT).show();
                    getGarbageInfo(labels);
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    isProcessing = false;

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setNavigationEnabled(true);
                    }
                    Toast.makeText(requireContext(), "Ошибка анализа изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void getGarbageInfo(List<String> labels) {
        geminiApiClient.getGarbageInfo(labels, new GeminiApiClient.GeminiApiListener() {
            @Override
            public void onSuccess(GarbageInfo garbageInfo) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    isProcessing = false;
                    showLoading(false);

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setNavigationEnabled(true);
                    }
                    displayResult(garbageInfo);

                    // Сохраняем результат сканирования в историю
                    currentWasteType = garbageInfo.getType();
                    currentInstructions = garbageInfo.getInstructions();
                    currentEstimatedCost = garbageInfo.getEstimatedCost();

                    if (currentImage != null) {
                        saveScanToHistory();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Ошибка получения информации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveScanToHistory() {
        // Создаем объект результата сканирования
        ScanResult scanResult = new ScanResult(
                System.currentTimeMillis(),
                currentWasteType,
                currentInstructions,
                currentEstimatedCost,
                currentImage
        );

        // Сохраняем в историю
        historyManager.addScanResult(scanResult);
    }

    private void showLoading(boolean isLoading) {
        Animation fadeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_out);

        if (isLoading) {
            progressBar.startAnimation(fadeAnimation);
            tvLoading.startAnimation(fadeAnimation);
            tvPlaceholder.setVisibility(View.GONE);
        }

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
        tvInstructions.setText("Инструкция: " + garbageInfo.getInstructions());
        tvEstimatedCost.setText("Оценочная стоимость: " + garbageInfo.getEstimatedCost());

        // Анимация появления результатов
        Animation slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);

        cardResult.setVisibility(View.VISIBLE);
        cardResult.startAnimation(slideUpAnimation);

        // Показываем кнопку поиска пунктов приема с анимацией
        btnFindRecyclingPoints.setVisibility(View.VISIBLE);
        btnFindRecyclingPoints.startAnimation(slideUpAnimation);

        // Сохраняем текущий тип отхода для передачи в MapActivity
        currentWasteType = garbageInfo.getType();
    }
}