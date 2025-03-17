package com.example.garbagescaner.remote.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisionApiClient {
    private static final String TAG = "VisionApiClient";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context context;

    public VisionApiClient(Context context) {
        this.context = context;
    }

    public interface VisionApiListener {
        void onSuccess(List<String> labels);
        void onError(Exception e);
    }

    public void analyzeImage(Bitmap bitmap, VisionApiListener listener) {
        executor.execute(() -> {
            try {
                // Load credentials
                InputStream credentialsStream = context.getAssets().open("garbagescaner-454017-827fa0fdc541.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                credentialsStream.close();

                // Set up the Vision API client
                ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

                // Create the image
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                ByteString imageBytes = ByteString.copyFrom(stream.toByteArray());
                Image image = Image.newBuilder().setContent(imageBytes).build();

                // Set up the request
                Feature feature = Feature.newBuilder()
                        .setType(Feature.Type.LABEL_DETECTION)
                        .setMaxResults(10)
                        .build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feature)
                        .setImage(image)
                        .build();

                // Process the request
                try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                    BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
                    AnnotateImageResponse imageResponse = response.getResponses(0);

                    List<String> labels = new ArrayList<>();
                    for (EntityAnnotation annotation : imageResponse.getLabelAnnotationsList()) {
                        labels.add(annotation.getDescription());
                    }

                    listener.onSuccess(labels);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error analyzing image", e);
                listener.onError(e);
            }
        });
    }
}
