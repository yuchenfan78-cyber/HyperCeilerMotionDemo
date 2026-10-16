package me.rain.kernel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.provision.renderengine.GlowController;
import com.sevtinge.hyperceiler.provision.renderengine.RenderViewLayout;

public final class MainActivity extends Activity {
    private static final String TAG = "RainKernel";
    private RenderViewLayout renderLayout;
    private GlowController glowController;
    private MotionAnimator motionAnimator;
    private LogoColorAnimator logoColors;
    private boolean started;
    private boolean launching;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        setContentView(R.layout.provision_startup_layout);
        renderLayout = findViewById(R.id.render_view_layout);
        View content = findViewById(R.id.miui_enter_layout);
        ImageView logo = findViewById(R.id.logo_image);
        ImageView wordmark = findViewById(R.id.text_logo_image);
        View logoWrapper = findViewById(R.id.logo_image_wrapper);
        View next = findViewById(R.id.next_layout);
        View glowLayer = new View(this);
        renderLayout.attachView(glowLayer, 0.2f, Color.BLACK);
        glowController = new GlowController(glowLayer);
        logo.setImageResource(R.drawable.rainkernel_logo);
        wordmark.setImageResource(R.drawable.rainkernel_wordmark);
        NativeBlur.setupContainer(content, dp(50));
        NativeBlur.setupLogo(logo);
        NativeBlur.setupLogo(wordmark);
        logoColors = new LogoColorAnimator(logo, wordmark);
        logo.setVisibility(View.INVISIBLE);
        logoWrapper.setVisibility(View.INVISIBLE);
        next.setVisibility(View.INVISIBLE);
        motionAnimator = new MotionAnimator(logo, logoWrapper, next);
        next.setOnClickListener(v -> {
            if (launching) return;
            launching = true;
            RoundedThumbnailTransition.start(this, next,
                    new Intent(this, PermissionSettingsActivity.class));
            next.postDelayed(() -> launching = false, 1000L);
        });
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (!started) {
            started = true;
            renderLayout.post(this::playIntro);
        }
    }

    private void playIntro() {
        try {
            glowController.stop();
            glowController.start(true);
            glowController.setCircleYOffsetWithView(
                    findViewById(R.id.logo_image_wrapper), renderLayout);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Shader initialization failed", exception);
            glowController.stop();
            View fallback = findViewById(R.id.background_image);
            fallback.setBackgroundColor(Color.BLACK);
            fallback.setVisibility(View.VISIBLE);
        }
        motionAnimator.play();
        logoColors.startOrResume();
    }

    @Override
    protected void onPause() {
        if (glowController != null) {
            if (launching) glowController.freeze();
            else glowController.stop();
        }
        if (logoColors != null) logoColors.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (started) launching = false;
        if (logoColors != null && started) logoColors.startOrResume();
        if (started && renderLayout != null) renderLayout.post(() -> {
            try {
                if (glowController != null) {
                    glowController.thaw();
                    glowController.start(false);
                }
                View next = findViewById(R.id.next_layout);
                if (next != null) {
                    next.setVisibility(View.VISIBLE);
                    next.setEnabled(true);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "Shader resume failed", error);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (motionAnimator != null) motionAnimator.cancel();
        if (logoColors != null) logoColors.destroy();
        if (glowController != null) glowController.stop();
        super.onDestroy();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
