package me.rain.kernel;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RoundedThumbnailTransition {
    public static final String REVEAL = "rain.reveal";
    public static final String CENTER_X = "rain.center_x";
    public static final String CENTER_Y = "rain.center_y";
    public static final String RADIUS = "rain.radius";
    private static final String TAG = "RainKernelTransition";
    private static final long PIXEL_COPY_TIMEOUT_MS = 700L;
    private static final long BUTTON_RESTORE_MS = 750L;

    private RoundedThumbnailTransition() {}

    public static void start(Activity activity, View button, Intent intent) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        int width = button.getWidth();
        int height = button.getHeight();
        int[] windowPosition = new int[2];
        int[] screenPosition = new int[2];
        button.getLocationInWindow(windowPosition);
        button.getLocationOnScreen(screenPosition);
        intent.putExtra(CENTER_X, screenPosition[0] + width / 2);
        intent.putExtra(CENTER_Y, screenPosition[1] + height / 2);
        intent.putExtra(RADIUS, Math.min(width, height) / 2);
        if (width <= 0 || height <= 0 || !button.isAttachedToWindow()
                || windowPosition[0] < 0 || windowPosition[1] < 0) {
            launch(activity, button, intent, null, windowPosition);
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean(false);
        Bitmap source;
        try {
            source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (IllegalArgumentException | OutOfMemoryError error) {
            Log.w(TAG, "Cannot allocate PixelCopy bitmap", error);
            launch(activity, button, intent, null, windowPosition);
            return;
        }

        Runnable timeout = () -> {
            if (completed.compareAndSet(false, true)) {
                // PixelCopy may still own the destination until its callback returns.
                launch(activity, button, intent, null, windowPosition);
            }
        };
        handler.postDelayed(timeout, PIXEL_COPY_TIMEOUT_MS);
        try {
            Rect area = new Rect(windowPosition[0], windowPosition[1],
                    windowPosition[0] + width, windowPosition[1] + height);
            PixelCopy.request(activity.getWindow(), area, source, result -> {
                if (!completed.compareAndSet(false, true)) {
                    source.recycle();
                    return;
                }
                handler.removeCallbacks(timeout);
                Bitmap rounded = null;
                try {
                    if (result == PixelCopy.SUCCESS) {
                        rounded = circleThumbnail(source,
                                button.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
                    } else {
                        Log.w(TAG, "PixelCopy returned " + result);
                    }
                } catch (RuntimeException | OutOfMemoryError error) {
                    Log.w(TAG, "Circle bitmap failed", error);
                } finally {
                    source.recycle();
                }
                launch(activity, button, intent, rounded, windowPosition);
            }, handler);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException error) {
            handler.removeCallbacks(timeout);
            if (completed.compareAndSet(false, true)) {
                source.recycle();
                Log.w(TAG, "PixelCopy cannot capture button", error);
                launch(activity, button, intent, null, windowPosition);
            }
        }
    }

    private static Bitmap circleThumbnail(Bitmap source, boolean rtl) {
        int side = Math.min(source.getWidth(), source.getHeight());
        if (side <= 0) return null;
        Bitmap out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Matrix matrix = new Matrix();
        float offsetX = (source.getWidth() - side) / 2f;
        float offsetY = (source.getHeight() - side) / 2f;
        if (rtl) {
            matrix.setScale(-1f, 1f);
            matrix.postTranslate(source.getWidth(), 0f);
        }
        matrix.postTranslate(-offsetX, -offsetY);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);
        canvas.drawCircle(side / 2f, side / 2f, side / 2f, paint);
        return out;
    }

    private static void launch(Activity activity, View button, Intent intent,
                               Bitmap thumbnail, int[] windowPosition) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            if (thumbnail != null) thumbnail.recycle();
            return;
        }
        boolean animated = thumbnail != null && thumbnail.getWidth() > 0
                && windowPosition[0] >= 0 && windowPosition[1] >= 0;
        ActivityOptions options = null;
        if (animated) {
            try {
                View decor = activity.getWindow().getDecorView();
                int[] decorPosition = new int[2];
                decor.getLocationInWindow(decorPosition);
                options = ActivityOptions.makeThumbnailScaleUpAnimation(decor, thumbnail,
                        windowPosition[0] - decorPosition[0],
                        windowPosition[1] - decorPosition[1]);
            } catch (RuntimeException error) {
                Log.w(TAG, "Thumbnail scale animation unavailable", error);
                animated = false;
            }
        }
        intent.putExtra(REVEAL, animated);
        button.setEnabled(false);
        button.setVisibility(View.INVISIBLE);
        try {
            if (animated && options != null) {
                activity.startActivity(intent, options.toBundle());
            } else {
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to start permission page", error);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
            return;
        } finally {
            if (thumbnail != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!thumbnail.isRecycled()) thumbnail.recycle();
                }, 1200L);
            }
        }
        button.postDelayed(() -> {
            if (button.isAttachedToWindow()) {
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
            }
        }, BUTTON_RESTORE_MS);
    }
}
