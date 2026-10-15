package me.rain.kernel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;

public final class PermissionSettingsActivity extends FragmentActivity {
    private boolean enteringHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        setContentView(R.layout.provision_detail_layout);

        View scene = findViewById(R.id.permission_scene);
        View detail = findViewById(R.id.provision_lyt);
        View dimScrim = findViewById(R.id.permission_transition_scrim);
        scene.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            detail.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        scene.requestApplyInsets();

        ((TextView) findViewById(R.id.provision_title))
                .setText(R.string.provision_permission_settings_title);
        ((ImageView) findViewById(R.id.preview_image))
                .setImageResource(R.drawable.provision_service_state);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { closeToSplash(); }
        });
        findViewById(R.id.back_icon).setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.provision_confirm).setOnClickListener(view -> nextPage());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.provision_container, new PermissionSettingsFragment())
                    .commitNow();
        }

        if (getIntent().getBooleanExtra(RoundedThumbnailTransition.REVEAL, false)
                && savedInstanceState == null) {
            scene.setVisibility(View.INVISIBLE);
            detail.setAlpha(0f);
            dimScrim.setVisibility(View.VISIBLE);
            scene.post(() -> beginCircleReveal(scene, detail, dimScrim));
        } else {
            dimScrim.setVisibility(View.GONE);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void beginCircleReveal(View scene, View detail, View dimScrim) {
        if (isFinishing() || isDestroyed()) return;
        int width = scene.getWidth();
        int height = scene.getHeight();
        if (width <= 0 || height <= 0) {
            scene.setVisibility(View.VISIBLE);
            detail.setAlpha(1f);
            dimScrim.setVisibility(View.GONE);
            return;
        }
        int[] origin = new int[2];
        scene.getLocationOnScreen(origin);
        int cx = getIntent().getIntExtra(RoundedThumbnailTransition.CENTER_X, width / 2) - origin[0];
        int cy = getIntent().getIntExtra(RoundedThumbnailTransition.CENTER_Y, height / 2) - origin[1];
        cx = Math.min(Math.max(0, cx), width);
        cy = Math.min(Math.max(0, cy), height);
        int radius = Math.max(1, getIntent().getIntExtra(RoundedThumbnailTransition.RADIUS, 36));
        float endRadius = (float) Math.hypot(
                Math.max(cx, width - cx), Math.max(cy, height - cy));
        if (endRadius <= radius) {
            scene.setVisibility(View.VISIBLE);
            detail.setAlpha(1f);
            dimScrim.setVisibility(View.GONE);
            return;
        }
        scene.setVisibility(View.VISIBLE);
        Animator reveal = ViewAnimationUtils.createCircularReveal(scene, cx, cy, radius, endRadius);
        reveal.setDuration(600L);
        reveal.setInterpolator(new DecelerateInterpolator(1.2f));
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                dimScrim.setVisibility(View.GONE);
                detail.setAlpha(1f);
            }
        });
        dimScrim.animate().alpha(0f).setStartDelay(80L).setDuration(510L).start();
        detail.animate().alpha(1f).setStartDelay(240L).setDuration(360L).start();
        reveal.start();
    }

    private void nextPage() {
        if (enteringHome) return;
        enteringHome = true;
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.SPRING_ENTRY, true);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void closeToSplash() {
        if (isFinishing()) return;
        finish();
        overridePendingTransition(R.anim.provision_slide_in_left,
                R.anim.provision_slide_out_right);
    }
}
