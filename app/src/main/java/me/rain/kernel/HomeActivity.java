package me.rain.kernel;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public final class HomeActivity extends ComponentActivity {
    public static final String SPRING_ENTRY = "rain.spring_entry";
    private static final int[] PAGE_TITLES = {
            R.string.tab_overview, R.string.tab_drivers,
            R.string.tab_profiles, R.string.tab_settings
    };
    private FrameLayout contentHost;
    private RainFloatingNav floatingNavigation;
    private int currentPage = -1;
    private boolean pageInMotion;
    private int bottomInset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        contentHost = findViewById(R.id.content_host);
        floatingNavigation = findViewById(R.id.floating_navigation);
        configureWindow();
        floatingNavigation.setOnNavigate(this::showPage);
        showPage(savedInstanceState == null ? 0 : savedInstanceState.getInt("page", 0));
        if (getIntent().getBooleanExtra(SPRING_ENTRY, false) && savedInstanceState == null) {
            View root = findViewById(R.id.home_root);
            root.setAlpha(0f);
            root.setScaleX(1.3f);
            root.setScaleY(1.3f);
            root.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                spring(root, SpringAnimation.SCALE_X);
                spring(root, SpringAnimation.SCALE_Y);
                root.animate().alpha(1f).setDuration(420L).start();
            });
        }
    }

    private void spring(View root, androidx.dynamicanimation.animation.FloatPropertyCompat<View> property) {
        SpringForce force = new SpringForce(1f)
                .setDampingRatio(0.65f)
                .setStiffness(130.5f);
        SpringAnimation animation = new SpringAnimation(root, property);
        animation.setSpring(force);
        animation.setStartValue(1.3f);
        animation.start();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
        View root = findViewById(R.id.home_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            bottomInset = bars.bottom;
            if (contentHost != null) contentHost.setPadding(0, 0, 0, dp(72) + bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void showPage(int page) {
        if (page < 0 || page >= PAGE_TITLES.length || currentPage == page) return;
        if (contentHost == null || floatingNavigation == null) return;
        if (pageInMotion) {
            contentHost.animate().cancel();
            for (int i = 0; i < contentHost.getChildCount(); i++) {
                contentHost.getChildAt(i).animate().cancel();
            }
            contentHost.removeAllViews();
            pageInMotion = false;
        }
        int previous = currentPage;
        currentPage = page;
        floatingNavigation.select(page);
        TextView title = new TextView(this);
        title.setText(PAGE_TITLES[page]);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(30f);
        title.setPadding(dp(24), dp(26), dp(24), 0);
        FrameLayout.LayoutParams parameters = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (previous < 0 || contentHost.getWidth() <= 0) {
            contentHost.removeAllViews();
            contentHost.addView(title, parameters);
            return;
        }
        int direction = page > previous ? 1 : -1;
        float offset = contentHost.getWidth() * direction;
        View old = contentHost.getChildAt(0);
        title.setTranslationX(offset);
        contentHost.addView(title, parameters);
        pageInMotion = true;
        title.animate().translationX(0).setDuration(350L)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (contentHost.indexOfChild(title) >= 0 && old != null) {
                        contentHost.removeView(old);
                    }
                    pageInMotion = false;
                }).start();
        if (old != null) {
            old.animate().translationX(-offset).setDuration(350L)
                    .setInterpolator(new LinearInterpolator()).start();
        }
    }

    private int dp(int size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("page", currentPage);
        super.onSaveInstanceState(state);
    }
}
