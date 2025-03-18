package com.example.garbagescaner;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private ImageCapture imageCapture;
    private PreviewView viewFinder;
    private CircularProgressIndicator progressBar;
    private TextView statusText;
    private View scanFrame;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private ValueAnimator scanFrameAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Настройка статус-бара и навигационного бара
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));

        try {
            // Инициализация UI компонентов
            viewFinder = findViewById(R.id.viewFinder);
            FloatingActionButton captureButton = findViewById(R.id.capture_button);
            progressBar = findViewById(R.id.progressBar);
            statusText = findViewById(R.id.status_text);
            scanFrame = findViewById(R.id.scanFrame);

            // Добавляем пульсирующую анимацию для рамки сканирования
            startScanFrameAnimation();

            // Проверка разрешений
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }

            // Настройка кнопки захвата
            captureButton.setOnClickListener(v -> takePhoto());

        } catch (Exception e) {
            // Ловим любые исключения при инициализации
            Log.e(TAG, "Ошибка при инициализации: ", e);
            Toast.makeText(this, "Ошибка инициализации: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startScanFrameAnimation() {
        // Создаем анимацию пульсации для рамки сканирования
        scanFrameAnimator = ValueAnimator.ofFloat(1.0f, 1.1f);
        scanFrameAnimator.setDuration(1000);
        scanFrameAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanFrameAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanFrameAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        scanFrameAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            scanFrame.setScaleX(value);
            scanFrame.setScaleY(value);
        });

        scanFrameAnimator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Останавливаем анимацию при уничтожении активности
        if (scanFrameAnimator != null && scanFrameAnimator.isRunning()) {
            scanFrameAnimator.cancel();
        }
    }

    private void startCamera() {
        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error starting camera: ", e);
                    Toast.makeText(this, "Не удалось запустить камеру", Toast.LENGTH_SHORT).show();
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске камеры: ", e);
            Toast.makeText(this, "Ошибка при запуске камеры: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        try {
            // Настройка preview
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            // Настройка селектора камеры (используем заднюю камеру)
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            // Настройка захвата изображения
            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            // Удаляем все привязки перед добавлением новых
            cameraProvider.unbindAll();

            // Привязываем камеру к жизненному циклу
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            if (statusText != null) {
                statusText.setText("Поместите отход в рамку");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка привязки камеры: ", e);
            Toast.makeText(this, "Ошибка инициализации камеры: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Камера не инициализирована", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Показываем прогресс
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (statusText != null) {
                statusText.setText("Делаем снимок...");
            }

            // Создаем директорию, если нужно
            File directory = getFilesDir();
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Создаем временный файл для фото с уникальным именем
            File photoFile = new File(directory, "waste_" + System.currentTimeMillis() + ".jpg");
            Log.d(TAG, "Сохраняем фото в: " + photoFile.getAbsolutePath());

            // Создаем выходные параметры
            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            // Делаем снимок
            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                            try {
                                if (statusText != null) {
                                    statusText.setText("Обработка изображения...");
                                }

                                // Проверяем, существует ли файл
                                if (!photoFile.exists() || photoFile.length() == 0) {
                                    Log.e(TAG, "Файл не был создан или пуст: " + photoFile.getAbsolutePath());
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    Toast.makeText(CameraActivity.this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Возвращаем результат в MainActivity
                                returnImageResult(photoFile.getAbsolutePath());

                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка после сохранения изображения: ", e);
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                Toast.makeText(CameraActivity.this, "Ошибка обработки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            Log.e(TAG, "Ошибка при сохранении фото: ", exception);
                            Toast.makeText(CameraActivity.this,
                                    "Ошибка при съемке фото: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            if (statusText != null) {
                                statusText.setText("Готов к сканированию");
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при съемке фото: ", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Ошибка при съемке фото: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void returnImageResult(String imagePath) {
        try {
            // Создаем Intent с результатом
            Intent resultIntent = new Intent();
            resultIntent.putExtra("image_path", imagePath);
            setResult(RESULT_OK, resultIntent);

            // Завершаем активность
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при возврате результата: ", e);
            Toast.makeText(this, "Ошибка при возврате результата", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Разрешения не получены", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}