package com.example.interim;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ColorAnalysisActivity extends AppCompatActivity {

    private View overlayView;
    private Button[] buttons;
    private FaceRecognitionOverlayView faceOverlayView;
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private String skinTone;
    private String season;

    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_analysis);

        overlayView = findViewById(R.id.overlayView);
        faceOverlayView = findViewById(R.id.faceOverlayView);
        buttons = new Button[]{
                findViewById(R.id.btnColor_1),
                findViewById(R.id.btnColor_2),
                findViewById(R.id.btnColor_3),
                findViewById(R.id.btnColor_4),
                findViewById(R.id.btnColor_5)
        };

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        requestCameraPermission();
        setupFaceDetector();
    }

    //  Request Camera Permission
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    //  Handle Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    // Start CameraX
    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(((androidx.camera.view.PreviewView) findViewById(R.id.cameraPreview)).getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //  Setup Face Detector
    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    //  Process Camera Image for Face Detection
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), rotation);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        RectF detectedFaceBounds = new RectF(
                                face.getBoundingBox().left,
                                face.getBoundingBox().top,
                                face.getBoundingBox().right,
                                face.getBoundingBox().bottom
                        );

                        faceOverlayView.setFaceBounds(detectedFaceBounds, imageProxy.getWidth(), imageProxy.getHeight());

                        // Extract skin tone and fetch AI-generated colors
                        extractSkinToneAndFetchColors(detectedFaceBounds);
                    }
                })
                .addOnFailureListener(e -> Log.e("FaceDetection", "Error detecting face", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    // Extract skin tone from face bounds (dummy method, replace with ML model)
    private void extractSkinToneAndFetchColors(RectF faceBounds) {
        // Simulate skin tone detection (replace with actual ML model)
        skinTone = "#E0A899"; // Dummy detected skin tone

        // Fetch AI-generated colors
        fetchAndApplyColors();
    }

    private void fetchAndApplyColors() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String[]> colorPairs = OpenAIService.getHexColors(skinTone, season);

            if (colorPairs.isEmpty()) {
                Log.e("ColorAnalysis", "No colors fetched from OpenAI.");
                return;
            }

            runOnUiThread(() -> {
                for (int i = 0; i < buttons.length && i < colorPairs.size(); i++) {
                    String colorName = colorPairs.get(i)[0];  // Color name
                    String hexCode = colorPairs.get(i)[1];    // Hex code

                    Log.d("ColorAnalysis", "Assigning " + colorName + " (" + hexCode + ") to button " + i);

                    buttons[i].setBackgroundColor(Color.parseColor(hexCode));
                    buttons[i].setText(colorName); // Display color name on button
                    buttons[i].setTextColor(isDarkColor(hexCode) ? Color.WHITE : Color.BLACK);

                    int finalI = i;
                    buttons[i].setOnClickListener(v -> {
                        Log.d("ColorAnalysis", "Setting background color: " + colorPairs.get(finalI)[1]);
                        overlayView.setBackgroundColor(Color.parseColor(colorPairs.get(finalI)[1]));
                    });

                    // Long press (1 second) to redirect to ProfileActivity
                    buttons[i].setOnLongClickListener(v -> {
                        Log.d("ColorAnalysis", "Long press detected on button " + finalI + ". Redirecting to ProfileActivity.");
                        Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                        v.getContext().startActivity(intent);
                        return true; // Indicates that the event is handled
                    });
                }
            });
        });
    }

    // Determine if a color is dark
    private boolean isDarkColor(String hex) {
        int color = Color.parseColor(hex);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        double brightness = (r * 0.299 + g * 0.587 + b * 0.114);
        return brightness < 128;
    }
}
