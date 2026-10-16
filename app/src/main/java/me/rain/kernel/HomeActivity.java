package me.rain.kernel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
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
    private static final long PAGE_DURATION = 350L;
    private static final PathInterpolator PAGE_EASING =
            new PathInterpolator(0.22f, 0f, 0.12f, 1f);
    private final View[] pages = new View[PAGE_TITLES.length];
    private FrameLayout contentHost;
    private RainFloatingNav floatingNavigation;
    private ValueAnimator pageAnimator;
    private int currentPage = -1;
    private int bottomInset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        contentHost = findViewById(R.id.content_host);
        floatingNavigation = findViewById(R.id.floating_navigation);
        configureWindow();
        buildPages();
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

    private void buildPages() {
        for (int i = 0; i < pages.length; i++) {
            TextView page = new TextView(this);
            page.setText(PAGE_TITLES[i]);
            page.setTextColor(getColor(R.color.text_primary));
            page.setTextSize(30f);
            page.setPadding(dp(24), dp(26), dp(24), 0);
            page.setVisibility(View.GONE);
            pages[i] = page;
            contentHost.addView(page, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private void spring(View root,
            androidx.dynamicanimation.animation.FloatPropertyCompat<View> property) {
        SpringForce force = new SpringForce(1f)
                .setDampingRatio(0.65f).setStiffness(130.5f);
        new SpringAnimation(root, property).setSpring(force).setStartValue(1.3f).start();
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

    private void showPage(int index) {
        if (index < 0 || index >= pages.length || currentPage == index) return;
        if (pageAnimator != null) {
            pageAnimator.cancel();
            pageAnimator = null;
        }
        int oldIndex = currentPage;
        // Normalize every page, including half-translated pages from interrupted gestures.
        for (View view : pages) {
            view.setTranslationX(0f);
            view.setAlpha(1f);
            view.setVisibility(View.GONE);
        }
        currentPage = index;
        floatingNavigation.select(index);
        View incoming = pages[index];
        incoming.setVisibility(View.VISIBLE);
        if (oldIndex < 0) return;
        final View outgoing = pages[oldIndex];
        outgoing.setVisibility(View.VISIBLE);
        incoming.bringToFront();
        final float travel = Math.max(1, contentHost.getWidth()) *
                (index > oldIndex ? 1f : -1f);
        incoming.setTranslationX(travel);
        incoming.setAlpha(0.75f);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        pageAnimator = animator;
        animator.setDuration(PAGE_DURATION);
        animator.setInterpolator(PAGE_EASING);
        animator.addUpdateListener(frame -> {
            float f = (float) frame.getAnimatedValue();
            incoming.setTranslationX(travel * (1f - f));
            incoming.setAlpha(0.75f + 0.25f * f);
            outgoing.setTranslationX(-travel * f);
            outgoing.setAlpha(1f - 0.25f * f);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (pageAnimator != animator) return;
                pageAnimator = null;
                outgoing.setVisibility(View.GONE);
                outgoing.setTranslationX(0f);
                outgoing.setAlpha(1f);
                incoming.setTranslationX(0f);
                incoming.setAlpha(1f);
            }
        });
        // Wait for first layout if the user taps during the very first frame.
        if (contentHost.getWidth() == 0) {
            incoming.setTranslationX(0f);
            outgoing.setVisibility(View.GONE);
            pageAnimator = null;
        } else animator.start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("page", currentPage);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (pageAnimator != null) pageAnimator.cancel();
        super.onDestroy();
    }
}
