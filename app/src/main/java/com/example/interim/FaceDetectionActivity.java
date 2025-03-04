package com.example.interim;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FaceDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView promptText;
    private TextView statusTextView;
    private FaceDetector faceDetector;
    private boolean isExpanded = false;
    private ExecutorService cameraExecutor;
    private AtomicBoolean isProcessing = new AtomicBoolean(false); // Prevents redundant alerts
    private CardView cameraCard; // Reference to CardView for border change

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);

        previewView = findViewById(R.id.cameraPreview);
        statusTextView = findViewById(R.id.faceStatusText);
        cameraCard = findViewById(R.id.cameraCard);
        promptText = findViewById(R.id.tapText);

        // Initialize Face Detector
        faceDetector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build());

        startCamera();

        cameraCard.setOnLongClickListener(v -> {
            shrinkAndReturnToCamera();
            return true;
        });


    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
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

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (!isProcessing.get()) {
                        isProcessing.set(true);
                        processImage(image);
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("FaceDetection", "Camera error", e);
            }
        }, ContextCompat.getMainExecutor(this));

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImage(ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            statusTextView.setText("Face detected!");
                            statusTextView.setVisibility(View.VISIBLE);
                            promptText.setText("Tap to proceed to skintone selection");
                            statusTextView.setVisibility(View.VISIBLE);

                            cameraCard.setOnClickListener(v -> {
                                Intent intent = new Intent(FaceDetectionActivity.this, SkintoneSelectionActivity.class);
                                startActivity(intent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            });
                        } else {
                            statusTextView.setText("Detecting face...");
                            promptText.setText("Press the screen to go back");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FaceDetection", "Face detection failed", e))
                    .addOnCompleteListener(task -> {
                        isProcessing.set(false);
                        imageProxy.close();
                    });
        }
    }



    private void shrinkAndReturnToCamera() {
        if (cameraCard == null) {
            Log.e("FaceDetection", "cameraCard is null!");
            return;
        }

        int startWidth = cameraCard.getWidth();
        int startHeight = cameraCard.getHeight();
        int endWidth = isExpanded ? ((ViewGroup) cameraCard.getParent()).getWidth() : dpToPx(300);
        int endHeight = isExpanded ? ((ViewGroup) cameraCard.getParent()).getHeight() : dpToPx(500);

        ValueAnimator widthAnimator = ValueAnimator.ofInt(startWidth, endWidth);
        ValueAnimator heightAnimator = ValueAnimator.ofInt(startHeight, endHeight);

        widthAnimator.setDuration(400);
        heightAnimator.setDuration(400);
        widthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        widthAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = cameraCard.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            cameraCard.setLayoutParams(params);
        });

        heightAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = cameraCard.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            cameraCard.setLayoutParams(params);
        });

        widthAnimator.start();
        heightAnimator.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(FaceDetectionActivity.this, CameraActivity.class);
            intent.putExtra("contract", true);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 450);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}
