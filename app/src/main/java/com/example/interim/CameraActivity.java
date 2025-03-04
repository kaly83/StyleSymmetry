package com.example.interim;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    private CardView cameraCard;
    private PreviewView previewView;
    private int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private boolean isContracted = false;

    private Button profileButton, cameraButton; // Toggle buttons

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera(cameraFacing);
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        RelativeLayout cameraflip = findViewById(R.id.main);
        previewView = findViewById(R.id.cameraPreview);
        cameraCard = findViewById(R.id.cameraCard);
        profileButton = findViewById(R.id.profileButton);
        cameraButton = findViewById(R.id.cameraButton);

        // Set "Camera" as active button by default
        setActiveButton(cameraButton, profileButton);


        // Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        // Flip Camera
        cameraflip.setOnClickListener(view -> toggleCamera());

        // Prevent touch interaction on the camera card
        cameraCard.setOnTouchListener((view, motionEvent) -> true);

        // Expand Preview on Click
        previewView.setOnClickListener(v -> toggleFullScreenPreview());

        // Toggle Buttons
        profileButton.setOnClickListener(v -> {
            setActiveButton(profileButton, cameraButton);
            Intent intent = new Intent(CameraActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        cameraButton.setOnClickListener(v -> setActiveButton(cameraButton, profileButton));
    }


    private void toggleFullScreenPreview() {
        int startWidth = cameraCard.getWidth();
        int startHeight = cameraCard.getHeight();

        int endWidth = isContracted ? dpToPx(300) : ((ViewGroup) cameraCard.getParent()).getWidth();
        int endHeight = isContracted ? dpToPx(500) : ((ViewGroup) cameraCard.getParent()).getHeight();

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

        // Fade out buttons when expanding, fade in when shrinking
        if (!isContracted) {
            profileButton.animate().alpha(0f).setDuration(200).start();
            cameraButton.animate().alpha(0f).setDuration(200).start();
            cameraCard.setRadius(0f);

            // 🚀 **Start FaceDetectionActivity after animation completes**
            heightAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    Intent intent = new Intent(CameraActivity.this, FaceDetectionActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });

        } else {
            profileButton.animate().alpha(1f).setDuration(200).start();
            cameraButton.animate().alpha(1f).setDuration(200).start();
            TextView tapText = findViewById(R.id.tapText);
            tapText.setText("Tap the camera to proceed"); // Set new text
            tapText.setVisibility(View.VISIBLE);
            tapText.setTextColor(getResources().getColor(R.color.black));
            tapText.setTextAppearance(android.R.style.TextAppearance_Medium);
            cameraCard.setRadius(50f);
        }

        widthAnimator.start();
        heightAnimator.start();

        isContracted = !isContracted;
    }



    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }


    private void toggleCamera() {
        cameraFacing = (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
        startCamera(cameraFacing);
    }

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = listenableFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(aspectRatio)
                        .build();

                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing)
                        .build();

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        return (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0))
                ? AspectRatio.RATIO_4_3
                : AspectRatio.RATIO_16_9;
    }

    private void setActiveButton(Button active, Button inactive) {
        active.setBackgroundResource(R.drawable.toggle_active);
        active.setTextColor(getResources().getColor(android.R.color.white));
        inactive.setBackgroundResource(R.drawable.toggle_inactive);
        inactive.setTextColor(getResources().getColor(android.R.color.black));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}

