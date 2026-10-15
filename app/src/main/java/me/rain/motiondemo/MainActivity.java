package me.rain.motiondemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.renderengine.GlowController;
import com.sevtinge.hyperceiler.provision.renderengine.RenderViewLayout;

public final class MainActivity extends Activity {
    private static final String TAG = "OriginalMotion";

    private View glowLayer;
    private View root;
    private RenderViewLayout renderLayout;
    private GlowController glowController;
    private MotionAnimator motionAnimator;
    private boolean started;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        setContentView(R.layout.provision_startup_layout);

        root = findViewById(android.R.id.content);
        renderLayout = findViewById(R.id.render_view_layout);
        View content = findViewById(R.id.miui_enter_layout);
        ImageView logo = findViewById(R.id.logo_image);
        ImageView wordmark = findViewById(R.id.text_logo_image);
        View logoWrapper = findViewById(R.id.logo_image_wrapper);
        View nextLayout = findViewById(R.id.next_layout);
        ImageButton nextButton = findViewById(R.id.next);
        ImageView nextArrow = findViewById(R.id.next_arrow);

        glowLayer = new View(this);
        renderLayout.attachView(glowLayer, 0.2f, Color.BLACK);
        glowController = new GlowController(glowLayer);

        logo.setImageResource(R.drawable.provision_logo_image);
        wordmark.setImageResource(R.drawable.provision_text_logo_image);
        nextButton.setBackgroundResource(R.drawable.provision_next);
        nextArrow.setImageResource(R.drawable.provision_icon_arrow);
        nextArrow.setVisibility(View.VISIBLE);

        NativeBlur.setupContainer(content, dp(50));
        NativeBlur.setupLogo(logo);
        NativeBlur.setupLogo(wordmark);
        if (!NativeBlur.setupButton(nextButton)) {
            nextButton.setBackgroundResource(R.drawable.provision_next_lite);
            nextArrow.setVisibility(View.GONE);
        }

        logo.setVisibility(View.INVISIBLE);
        logoWrapper.setVisibility(View.INVISIBLE);
        nextLayout.setVisibility(View.INVISIBLE);
        motionAnimator = new MotionAnimator(logo, logoWrapper, nextLayout);

        nextLayout.setOnClickListener(v -> replay());
        nextButton.setOnClickListener(v -> replay());
        nextArrow.setOnClickListener(v -> replay());
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
            renderLayout.post(this::replay);
        }
    }

    private void replay() {
        if (glowController != null) {
            try {
                glowController.stop();
                glowController.start(true);
                glowController.setCircleYOffsetWithView(
                    findViewById(R.id.logo_image_wrapper), renderLayout);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Original shader failed", exception);
                glowController.stop();
                findViewById(R.id.background_image).setVisibility(View.VISIBLE);
                ((ImageView) findViewById(R.id.background_image))
                    .setImageResource(R.drawable.provision_logo_image_bg);
            }
        }
        motionAnimator.play();
    }

    @Override
    protected void onPause() {
        if (glowController != null) glowController.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (started && root != null) root.post(this::resumeGlow);
    }

    private void resumeGlow() {
        if (glowController == null || renderLayout == null) return;
        try {
            glowController.start(false);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to resume shader", exception);
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
