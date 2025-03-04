package com.example.interim;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkintoneSelectionActivity extends AppCompatActivity {

    private androidx.camera.view.PreviewView previewView;
    private View overlayView;
    private FaceRecognitionOverlayView faceOverlayView;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private RectF detectedFaceBounds = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private String selectedSkinTone = "#F4C2C2"; // Default light skin tone
    private String selectedSeason = "Spring"; // Default season


    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skintone_selection);

        previewView = findViewById(R.id.cameraPreview);
        overlayView = findViewById(R.id.overlayView);
        faceOverlayView = findViewById(R.id.faceOverlayView);

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        requestCameraPermission();
        setupSkinToneButtons();
        setupFaceDetector();
        setupSkinToneButtons();
    }

    // Request Camera
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

    //Start Camera
    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

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

    // Setup Face Detector
    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()  // Track faces more effectively
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

        // Ensure correct rotation
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), rotation);
        Log.d("FaceDetection", "Image rotation: " + rotation);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d("FaceDetection", "Faces detected: " + faces.size());

                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        RectF detectedFaceBounds = new RectF(
                                face.getBoundingBox().left,
                                face.getBoundingBox().top,
                                face.getBoundingBox().right,
                                face.getBoundingBox().bottom
                        );

                        Log.d("FaceDetection", "Face Bounding Box: " + detectedFaceBounds);

                        // Get camera preview size (Assuming you've set these somewhere)
                        int previewWidth = imageProxy.getWidth();
                        int previewHeight = imageProxy.getHeight();

                        // Pass detected face & camera preview size to overlay
                        faceOverlayView.setFaceBounds(detectedFaceBounds, previewWidth, previewHeight);
                    } else {
                        faceOverlayView.setFaceBounds(null, 0, 0);
                        Log.d("FaceDetection", "No face detected.");
                    }
                })
                .addOnFailureListener(e -> Log.e("FaceDetection", "Error detecting face", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }



    // Setup Skin Tone Buttons
    private void setupSkinToneButtons() {
        int[] buttonIds = {
                R.id.btn_spr_f, R.id.btn_spr_t, R.id.btn_spr_d,  // Spring
                R.id.btn_aut_f, R.id.btn_aut_t, R.id.btn_aut_d,  // Autumn
                R.id.btn_summ_f, R.id.btn_summ_t, R.id.btn_summ_d,  // Summer
                R.id.btn_wint_f, R.id.btn_wint_t, R.id.btn_wint_d   // Winter
        };

        int[] colorIds = {
                R.color.f_spr, R.color.t_spr, R.color.d_spr,  // Spring
                R.color.f_aut, R.color.t_aut, R.color.d_aut,  // Autumn
                R.color.f_summ, R.color.t_summ, R.color.d_summ,  // Summer
                R.color.f_wint, R.color.t_wint, R.color.d_wint   // Winter
        };

        String[] seasons = {
                "Spring", "Spring", "Spring",
                "Autumn", "Autumn", "Autumn",
                "Summer", "Summer", "Summer",
                "Winter", "Winter", "Winter"
        };

        for (int i = 0; i < buttonIds.length; i++) {
            Button button = findViewById(buttonIds[i]);
            int color = ContextCompat.getColor(this, colorIds[i]);
            String season = seasons[i];

            // Click to change background color
            button.setOnClickListener(v -> updateBackground(color));

            // Long press (1 second) to proceed
            button.setOnTouchListener(new View.OnTouchListener() {
                private Handler handler = new Handler();
                private Runnable longPressRunnable = () -> {
                    selectedSkinTone = String.format("#%06X", (0xFFFFFF & color)); // Store hex color
                    selectedSeason = season; // Store season

                    // Automatically proceed to the next activity
                    Intent intent = new Intent(SkintoneSelectionActivity.this, ColorAnalysisActivity.class);
                    intent.putExtra("SKIN_TONE", selectedSkinTone);
                    intent.putExtra("SEASON", selectedSeason);
                    startActivity(intent);

                    SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("selectedSkinTone", selectedSkinTone); // Example: "Light Olive"
                    editor.putString("selectedSeason", selectedSeason); // Example: "Cool Autumn"
                    editor.apply();

                };

                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            handler.postDelayed(longPressRunnable, 1000); // 1 second delay
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(longPressRunnable); // Cancel if released early
                            break;
                    }
                    return false;
                }
            });

        }

    }







    // Update Background (excluding face)
    private void updateBackground(int color) {
        overlayView.setBackgroundColor(color);
        faceOverlayView.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }



}
