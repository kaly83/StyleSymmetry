package com.example.interim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.face.Face;

public class FaceOverlayView extends View {
    private Face detectedFace;
    private final Paint paint;

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
    }

    public void setDetectedFace(Face face) {
        this.detectedFace = face;
        invalidate(); // Redraw the view with the new face
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (detectedFace != null) {
            Rect bounds = detectedFace.getBoundingBox();
            canvas.drawRect(bounds, paint);
        }
    }
}
