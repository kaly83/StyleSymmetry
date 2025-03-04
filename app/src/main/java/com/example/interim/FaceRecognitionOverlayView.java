package com.example.interim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class FaceRecognitionOverlayView extends View {
    private Paint overlayPaint;
    private Paint clearPaint;
    private RectF faceBounds;
    private boolean isMirrored = true; // Set to true if using a front-facing camera

    public FaceRecognitionOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(0);  // Semi-transparent overlay

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setFaceBounds(RectF faceBounds, int cameraPreviewWidth, int cameraPreviewHeight) {
        if (faceBounds == null) {
            this.faceBounds = null;
        } else {
            this.faceBounds = mapFaceBounds(faceBounds, cameraPreviewWidth, cameraPreviewHeight);
        }
        invalidate();
    }


    private RectF mapFaceBounds(RectF face, int previewWidth, int previewHeight) {
        float scaleX = (float) getWidth() / previewWidth;  // Scale width from camera to screen
        float scaleY = (float) getHeight() / previewHeight; // Scale height from camera to screen

        float left = face.left * scaleX;
        float right = face.right * scaleX;
        float top = face.top * scaleY;
        float bottom = face.bottom * scaleY;

        if (isMirrored) { // Flip horizontally for front-facing camera
            float tempLeft = left;
            left = getWidth() - right;
            right = getWidth() - tempLeft;
        }

        // Move the face cutout slightly UP
        float verticalOffset = (bottom - top) * 0.15f;  // Adjust this value if needed
        RectF adjustedFaceBounds = new RectF(left, top - verticalOffset, right, bottom - verticalOffset);


        // Log preview and screen sizes for debugging
        Log.d("FaceOverlay", "Preview Size: " + previewWidth + "x" + previewHeight);
        Log.d("FaceOverlay", "Screen Size: " + getWidth() + "x" + getHeight());
        Log.d("FaceOverlay", "Mapped Face Bounds: " + adjustedFaceBounds.toString());

        return adjustedFaceBounds;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faceBounds == null) return;

        // Draw semi-transparent overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Cut out circular area for face
        float radius = (faceBounds.width() + faceBounds.height()) / 4; // Average for better fit
        canvas.drawCircle(faceBounds.centerX(), faceBounds.centerY(), radius, clearPaint);
    }

}
