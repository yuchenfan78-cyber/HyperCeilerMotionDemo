package me.rain.kernel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private boolean started;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("rainkernel", MODE_PRIVATE);
        if (!getIntent().getBooleanExtra("preview_splash", false)
                && !prefs.getBoolean("show_splash", true)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
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

        logo.setVisibility(View.INVISIBLE);
        logoWrapper.setVisibility(View.INVISIBLE);
        next.setVisibility(View.INVISIBLE);
        motionAnimator = new MotionAnimator(logo, logoWrapper, next);
        next.setOnClickListener(v -> onNext());
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        immersive();
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
    }

    private void onNext() {
        View next = findViewById(R.id.next_layout);
        next.setEnabled(false);
        if (!getIntent().getBooleanExtra("preview_splash", false)) {
            startActivity(new Intent(this, HomeActivity.class));
        }
        overridePendingTransition(R.anim.push_in, R.anim.push_out);
        finish();
    }

    @Override
    protected void onPause() {
        if (glowController != null) glowController.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (started && renderLayout != null) renderLayout.post(this::resumeGlow);
    }

    private void resumeGlow() {
        try {
            if (glowController != null) glowController.start(false);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Shader resume failed", exception);
        }
    }

    @Override
    protected void onDestroy() {
        if (motionAnimator != null) motionAnimator.cancel();
        if (glowController != null) glowController.stop();
        super.onDestroy();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
