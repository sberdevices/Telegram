package org.telegram.ui.Components.voip;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.ui.Components.LayoutHelper;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

public class VoIPTextureView extends FrameLayout {

    final Path path = new Path();
    final Paint xRefPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    final boolean isCamera;

    float roundRadius;

    public final SurfaceViewRenderer renderer;
    public View backgroundView;

    public VoIPTextureView(@NonNull Context context, boolean isCamera) {
        super(context);
        this.isCamera = isCamera;
        renderer = new SurfaceViewRenderer(context) {
            @Override
            public void onFirstFrameRendered() {
                super.onFirstFrameRendered();
                VoIPTextureView.this.invalidate();
            }
        };
        renderer.setEnableHardwareScaler(true);
        renderer.setIsCamera(isCamera);
        if (!isCamera) {
            backgroundView = new View(context);
            backgroundView.setBackgroundColor(0xff1b1f23);
            addView(backgroundView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            addView(renderer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        } else {
            addView(renderer);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    if (roundRadius < 1) {
                        outline.setRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                    } else {
                        outline.setRoundRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), roundRadius);
                    }
                }
            });
            setClipToOutline(true);
        } else {
            xRefPaint.setColor(0xff000000);
            xRefPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (roundRadius > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                super.dispatchDraw(canvas);
                canvas.drawPath(path, xRefPaint);
            } catch (Exception ignore) {

            }
        } else {
            super.dispatchDraw(canvas);
        }
    }

    public void setRoundCorners(float radius) {
        roundRadius = radius;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            invalidateOutline();
        } else {
            invalidate();
        }
    }

    public void saveCameraLastBitmap() { }

    public void setStub(VoIPTextureView from) { }
}
