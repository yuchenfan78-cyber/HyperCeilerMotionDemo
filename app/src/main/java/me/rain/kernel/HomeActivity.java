package me.rain.kernel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.system.Os;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import fan.animation.Folme;

public final class HomeActivity extends ComponentActivity {
    private static final int[] PAGE_LAYOUTS = {
        R.layout.page_overview, R.layout.page_drivers,
        R.layout.page_profiles, R.layout.page_settings
    };
    private FrameLayout contentHost;
    private RainFloatingNav floatingNavigation;
    private int navigationInsetBottom;
    private SharedPreferences preferences;
    private int currentPage = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("rainkernel", MODE_PRIVATE);
        setContentView(R.layout.activity_home);
        configureWindow();
        contentHost = findViewById(R.id.content_host);
        floatingNavigation = findViewById(R.id.floating_navigation);
        floatingNavigation.setOnNavigate(this::showPage);
        if (savedInstanceState != null) {
            currentPage = -1;
            showPage(savedInstanceState.getInt("page", 0));
        } else {
            showPage(0);
        }
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
            navigationInsetBottom = bars.bottom;
            updateScrollPadding();
            return insets;
        });
        root.requestApplyInsets();
        int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int flags = night == Configuration.UI_MODE_NIGHT_YES ? 0
            : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void showPage(int page) {
        if (page < 0 || page >= PAGE_LAYOUTS.length || currentPage == page) return;
        int direction = currentPage < 0 ? 0 : (page > currentPage ? 1 : -1);
        currentPage = page;
        floatingNavigation.select(page);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setPadding(0, 0, 0, dp(94) + navigationInsetBottom);
        View pageContent = LayoutInflater.from(this).inflate(PAGE_LAYOUTS[page], scroller, false);
        scroller.addView(pageContent);
        contentHost.removeAllViews();
        contentHost.addView(scroller, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bindPage(page, pageContent);
        if (direction != 0 && preferences.getBoolean("page_animations", true)) {
            scroller.setTranslationX(dp(24) * direction);
            scroller.setAlpha(0.7f);
            scroller.animate().translationX(0).alpha(1f).setDuration(180)
                .setInterpolator(new LinearInterpolator()).start();
        }
    }

    private void updateScrollPadding() {
        if (contentHost == null || contentHost.getChildCount() == 0) return;
        View scroll = contentHost.getChildAt(0);
        if (scroll instanceof ScrollView) {
            scroll.setPadding(0, 0, 0, dp(94) + navigationInsetBottom);
        }
    }

    private void bindPage(int page, View root) {
        if (page == 0) {
            setText(root, R.id.device_model, Build.MANUFACTURER + " " + Build.MODEL);
            setText(root, R.id.android_version, "Android " + Build.VERSION.RELEASE + "  ·  API " + Build.VERSION.SDK_INT);
            String release;
            try {
                release = Os.uname().release;
            } catch (RuntimeException exception) {
                release = System.getProperty("os.version", getString(R.string.unknown));
            }
            setText(root, R.id.kernel_version, release == null ? getString(R.string.unknown) : release);
            wireNavigation(root, R.id.action_drivers, 1);
            wireNavigation(root, R.id.action_profiles, 2);
        } else if (page == 2) {
            bindProfile(root, R.id.profile_balanced, "balanced");
            bindProfile(root, R.id.profile_performance, "performance");
            bindProfile(root, R.id.profile_battery, "battery");
            updateProfile(root);
        } else if (page == 3) {
            Switch splashSwitch = root.findViewById(R.id.switch_splash);
            Switch motionSwitch = root.findViewById(R.id.switch_motion);
            splashSwitch.setChecked(preferences.getBoolean("show_splash", true));
            motionSwitch.setChecked(preferences.getBoolean("page_animations", true));
            splashSwitch.setOnCheckedChangeListener((button, enabled) ->
                preferences.edit().putBoolean("show_splash", enabled).apply());
            motionSwitch.setOnCheckedChangeListener((button, enabled) ->
                preferences.edit().putBoolean("page_animations", enabled).apply());
            View replay = root.findViewById(R.id.action_replay);
            replay.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("preview_splash", true);
                startActivity(intent);
                overridePendingTransition(R.anim.push_in, R.anim.push_out);
            });
            attachFolme(replay);
        }
    }

    private void setText(View root, int id, String content) {
        ((TextView) root.findViewById(id)).setText(content);
    }

    private void wireNavigation(View root, int id, int page) {
        View action = root.findViewById(id);
        action.setOnClickListener(v -> showPage(page));
        attachFolme(action);
    }

    private void attachFolme(View view) {
        Folme.useAt(view).touch().setScale(0.98f).handleTouchOf(view);
    }

    private void bindProfile(View root, int id, String key) {
        View item = root.findViewById(id);
        item.setOnClickListener(v -> {
            preferences.edit().putString("preview_profile", key).apply();
            updateProfile(root);
            Toast.makeText(this, R.string.profile_preview_only, Toast.LENGTH_SHORT).show();
        });
        attachFolme(item);
    }

    private void updateProfile(View root) {
        String key = preferences.getString("preview_profile", "balanced");
        String[] keys = {"balanced", "performance", "battery"};
        int[] views = {R.id.profile_balanced, R.id.profile_performance, R.id.profile_battery};
        for (int i = 0; i < views.length; i++) {
            View card = root.findViewById(views[i]);
            card.setSelected(keys[i].equals(key));
            TextView marker = card.findViewById(R.id.profile_selection);
            marker.setText(keys[i].equals(key) ? R.string.preview_selected : R.string.preview_select);
        }
    }

    private int dp(int dimension) {
        return Math.round(dimension * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("page", currentPage);
        super.onSaveInstanceState(outState);
    }
}
