package com.example.garbagescaner.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // TTS компоненты
    private Button btnReadResult;
    private MediaPlayer mediaPlayer;
    private ExecutorService ttsExecutor;
    private boolean isTtsProcessing = false;
    private File cacheDir;

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

        // Инициализируем executor для TTS
        ttsExecutor = Executors.newSingleThreadExecutor();

        // Инициализируем директорию кэша
        cacheDir = requireContext().getCacheDir();

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

        // Анимация для кнопки чтения
        if (btnReadResult != null) {
            btnReadResult.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.startAnimation(pulseAnimation);
                }
                return false;
            });
        }
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

        // Ищем кнопку чтения в макете
        btnReadResult = view.findViewById(R.id.btnReadResult);
        if (btnReadResult == null) {
            // Если кнопка отсутствует в макете, добавим ее программно
            btnReadResult = new Button(requireContext());
            btnReadResult.setId(View.generateViewId());
            btnReadResult.setText("Прочитать результат");

            // Проверяем наличие ресурса иконки
            try {
                requireContext().getResources().getDrawable(R.drawable.ic_speaker, null);
                btnReadResult.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_speaker, 0, 0, 0);
                btnReadResult.setCompoundDrawablePadding(8);
            } catch (Exception e) {
                Log.w(TAG, "Иконка динамика не найдена: " + e.getMessage());
            }

            // Добавляем кнопку после cardResult
            ViewGroup parent = (ViewGroup) cardResult.getParent();
            int index = parent.indexOfChild(cardResult);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(32, 16, 32, 16);

            // Добавляем кнопку в родительский ViewGroup
            parent.addView(btnReadResult, index + 1, params);
        }

        // Изначально скрываем кнопку
        btnReadResult.setVisibility(View.GONE);
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

        // Слушатель для кнопки чтения
        btnReadResult.setOnClickListener(v -> {
            if (currentWasteType != null) {
                speakFullGarbageInfo(); // Изменено на метод для полного озвучивания
            }
        });
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
                    isProcessing = false;

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).setNavigationEnabled(true);
                    }

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
        btnReadResult.setVisibility(View.GONE);
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

        // Показываем кнопку чтения результата
        btnReadResult.setVisibility(View.VISIBLE);
        btnReadResult.startAnimation(slideUpAnimation);

        // Сохраняем текущий тип отхода для передачи в MapActivity
        currentWasteType = garbageInfo.getType();

        // Автоматически озвучиваем только тип отхода при первом сканировании
        speakGarbageInfo();
    }

    /**
     * Преобразует тип отхода в текст для озвучивания с правильным склонением
     */
    private String getTextForSpeech() {
        if (currentWasteType == null || currentWasteType.isEmpty()) {
            return "Тип отхода не определен";
        }

        // Формируем правильное склонение в зависимости от типа отхода
        String result = "Определен тип отхода: ";

        switch (currentWasteType.toLowerCase()) {
            case "пластик":
                result += "пластик";
                break;
            case "стекло":
                result += "стекло";
                break;
            case "бумага":
                result += "бумага";
                break;
            case "металл":
                result += "металл";
                break;
            case "электроника":
                result += "электроника";
                break;
            case "пищевые отходы":
                result += "пищевые отходы";
                break;
            default:
                result += currentWasteType;
                break;
        }

        return result;
    }

    /**
     * Формирует полный текст для озвучивания всей информации о отходе
     */
    private String getFullTextForSpeech() {
        if (currentWasteType == null || currentWasteType.isEmpty()) {
            return "Тип отхода не определен";
        }

        StringBuilder result = new StringBuilder();

        // Добавляем тип отхода
        result.append("Определен тип отхода: ").append(currentWasteType).append(". ");

        // Добавляем инструкцию, если она есть
        if (currentInstructions != null && !currentInstructions.isEmpty()) {
            result.append("Инструкция по подготовке: ").append(currentInstructions).append(". ");
        }

        // Добавляем стоимость, если она есть
        if (currentEstimatedCost != null && !currentEstimatedCost.isEmpty()) {
            result.append("Оценочная стоимость: ").append(currentEstimatedCost).append(".");
        }

        return result.toString();
    }

    /**
     * Озвучивает только тип отхода (для автоматического озвучивания при первом сканировании)
     */
    private void speakGarbageInfo() {
        // Проверяем, не идет ли уже процесс синтеза речи
        if (isTtsProcessing) {
            Log.d(TAG, "Синтез речи уже выполняется, возвращаемся");
            return;
        }

        // Проверяем, не воспроизводится ли уже аудио
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.d(TAG, "Аудио уже воспроизводится, останавливаем");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Создаем имя файла, зависящее от типа отхода
        String safeWasteType = currentWasteType.replace(" ", "_").toLowerCase();
        File wasteTypeFile = new File(cacheDir, "tts_type_" + safeWasteType + ".mp3");

        // Проверяем, существует ли уже файл для этого типа отхода
        if (wasteTypeFile.exists() && wasteTypeFile.length() > 0) {
            // Если файл уже есть для текущего типа отхода, используем его
            Log.d(TAG, "Найден существующий аудиофайл для типа " + currentWasteType);
            playAudio(wasteTypeFile);
            return;
        }

        // Отображаем прогресс
        Toast.makeText(requireContext(), "Подготовка голосового сообщения...", Toast.LENGTH_SHORT).show();

        isTtsProcessing = true;
        final String textToSpeak = getTextForSpeech();

        // Запускаем в отдельном потоке
        ttsExecutor.execute(() -> {
            try {
                Log.d(TAG, "Начинаем синтез речи для текста: " + textToSpeak);

                // Загружаем учетные данные Google Cloud
                InputStream credentialsStream = requireContext().getAssets().open("garbagescaner-454017-827fa0fdc541.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                credentialsStream.close();

                // Настраиваем клиент Text-to-Speech
                TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

                // Подготавливаем параметры запроса
                try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
                    SynthesisInput input = SynthesisInput.newBuilder()
                            .setText(textToSpeak)
                            .build();

                    // Настраиваем голос (русский, мужской, Chirp3-HD-Charon)
                    VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                            .setLanguageCode("ru-RU")
                            .setName("ru-RU-Chirp3-HD-Charon")
                            .build();

                    // Настраиваем аудио (MP3)
                    AudioConfig audioConfig = AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.MP3)
                            .setSpeakingRate(0.9) // Немного замедляем речь для лучшего восприятия
                            .setPitch(0.0) // Нормальная высота голоса
                            .build();

                    // Синтезируем речь
                    Log.d(TAG, "Отправляем запрос на Google Cloud TTS");
                    SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                    ByteString audioContents = response.getAudioContent();

                    // Сохраняем аудио в файл для текущего типа отхода
                    Log.d(TAG, "Сохраняем аудио в файл: " + wasteTypeFile.getAbsolutePath());
                    try (OutputStream out = new FileOutputStream(wasteTypeFile)) {
                        out.write(audioContents.toByteArray());
                    }

                    // Воспроизводим аудио на главном потоке
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            playAudio(wasteTypeFile);
                            isTtsProcessing = false;
                        });
                    } else {
                        isTtsProcessing = false;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Ошибка синтеза речи: " + e.getMessage(), e);
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Ошибка голосового вывода: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isTtsProcessing = false;
                    });
                } else {
                    isTtsProcessing = false;
                }
            }
        });
    }

    /**
     * Озвучивает полную информацию о отходе (тип, инструкцию, стоимость)
     * Вызывается при нажатии на кнопку "Прочитать результат"
     */
    private void speakFullGarbageInfo() {
        // Проверяем, не идет ли уже процесс синтеза речи
        if (isTtsProcessing) {
            Log.d(TAG, "Синтез речи уже выполняется, возвращаемся");
            return;
        }

        // Проверяем, не воспроизводится ли уже аудио
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.d(TAG, "Аудио уже воспроизводится, останавливаем");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Создаем имя файла, зависящее от типа отхода и содержащее хеш текста
        String safeWasteType = currentWasteType.replace(" ", "_").toLowerCase();
        String fullText = getFullTextForSpeech();
        int textHash = fullText.hashCode();
        File fullInfoFile = new File(cacheDir, "tts_full_" + safeWasteType + "_" + textHash + ".mp3");

        // Проверяем, существует ли уже файл для полной информации
        if (fullInfoFile.exists() && fullInfoFile.length() > 0) {
            // Если файл уже есть, используем его
            Log.d(TAG, "Найден существующий аудиофайл для полной информации о " + currentWasteType);
            playAudio(fullInfoFile);
            return;
        }

        // Отображаем прогресс
        Toast.makeText(requireContext(), "Подготовка полного голосового описания...", Toast.LENGTH_SHORT).show();

        isTtsProcessing = true;

        // Запускаем в отдельном потоке
        ttsExecutor.execute(() -> {
            try {
                Log.d(TAG, "Начинаем синтез речи для полного текста: " + fullText);

                // Загружаем учетные данные Google Cloud
                InputStream credentialsStream = requireContext().getAssets().open("garbagescaner-454017-827fa0fdc541.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                credentialsStream.close();

                // Настраиваем клиент Text-to-Speech
                TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

                // Подготавливаем параметры запроса
                try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
                    SynthesisInput input = SynthesisInput.newBuilder()
                            .setText(fullText)
                            .build();

                    // Настраиваем голос (русский, мужской, Chirp3-HD-Charon)
                    VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                            .setLanguageCode("ru-RU")
                            .setName("ru-RU-Chirp3-HD-Charon")
                            .build();

                    // Настраиваем аудио (MP3)
                    AudioConfig audioConfig = AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.MP3)
                            .setSpeakingRate(0.9) // Немного замедляем речь для лучшего восприятия
                            .setPitch(0.0) // Нормальная высота голоса
                            .build();

                    // Синтезируем речь
                    Log.d(TAG, "Отправляем запрос на Google Cloud TTS для полного текста");
                    SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
                    ByteString audioContents = response.getAudioContent();

                    // Сохраняем аудио в файл для полной информации
                    Log.d(TAG, "Сохраняем полное аудио в файл: " + fullInfoFile.getAbsolutePath());
                    try (OutputStream out = new FileOutputStream(fullInfoFile)) {
                        out.write(audioContents.toByteArray());
                    }

                    // Воспроизводим аудио на главном потоке
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            playAudio(fullInfoFile);
                            isTtsProcessing = false;
                        });
                    } else {
                        isTtsProcessing = false;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Ошибка синтеза речи для полного текста: " + e.getMessage(), e);
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Ошибка голосового вывода: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isTtsProcessing = false;
                    });
                } else {
                    isTtsProcessing = false;
                }
            }
        });
    }

    /**
     * Воспроизводит аудиофайл с помощью MediaPlayer
     */
    private void playAudio(File audioFile) {
        try {
            // Освобождаем ресурсы, если медиаплеер уже использовался
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Создаем новый медиаплеер
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            // Настраиваем источник аудио
            mediaPlayer.setDataSource(requireContext(), Uri.fromFile(audioFile));
            mediaPlayer.prepare();

            // Добавляем слушатель завершения для освобождения ресурсов
            mediaPlayer.setOnCompletionListener(mp -> {
                // После завершения освобождаем ресурсы
                mp.release();
                mediaPlayer = null;
            });

            // Слушатель ошибок
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Ошибка воспроизведения: " + what + ", " + extra);
                Toast.makeText(requireContext(), "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                mp.release();
                mediaPlayer = null;
                return true;
            });

            // Запускаем воспроизведение
            mediaPlayer.start();
            Log.d(TAG, "Воспроизведение аудио начато");
        } catch (IOException e) {
            Log.e(TAG, "Ошибка воспроизведения аудио: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();

            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Останавливаем воспроизведение при приостановке фрагмента
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        // Освобождаем ресурсы медиаплеера при уничтожении фрагмента
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Завершаем executor
        if (ttsExecutor != null && !ttsExecutor.isShutdown()) {
            ttsExecutor.shutdown();
        }

        super.onDestroy();
    }
}