package me.rain.motiondemo;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

public final class MainActivity extends Activity {
    private GlowView glowView;
    private MotionController motionController;
    private boolean hasPlayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        setContentView(R.layout.activity_main);

        FrameLayout scene = findViewById(R.id.scene);
        View heading = findViewById(R.id.eyebrow);
        View brand = findViewById(R.id.brand);
        View replay = findViewById(R.id.replay_group);
        glowView = findViewById(R.id.glow);
        motionController = new MotionController(brand, replay);

        scene.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            FrameLayout.LayoutParams top = (FrameLayout.LayoutParams) heading.getLayoutParams();
            top.topMargin = dp(26) + bars.top;
            heading.setLayoutParams(top);

            FrameLayout.LayoutParams bottom = (FrameLayout.LayoutParams) replay.getLayoutParams();
            bottom.bottomMargin = dp(46) + bars.bottom;
            replay.setLayoutParams(bottom);
            return insets;
        });
        scene.requestApplyInsets();

        replay.setOnClickListener(view -> replay());
        scene.setOnClickListener(view -> replay());
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
    }

    private void updateSystemBarsAppearance() {
        View decorView = getWindow().getDecorView();
        if (!decorView.isAttachedToWindow()) return;

        WindowInsetsController controller = decorView.getWindowInsetsController();
        if (controller == null) return;

        boolean isNight = (getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        controller.setSystemBarsAppearance(isNight ? 0 : lightBars, lightBars);
    }

    private void replay() {
        glowView.replay();
        motionController.replay();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) updateSystemBarsAppearance();
        if (hasFocus && !hasPlayed) {
            hasPlayed = true;
            findViewById(R.id.scene).post(this::replay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glowView != null) glowView.start();
    }

    @Override
    protected void onPause() {
        if (glowView != null) glowView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (motionController != null) motionController.cancel();
        super.onDestroy();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
