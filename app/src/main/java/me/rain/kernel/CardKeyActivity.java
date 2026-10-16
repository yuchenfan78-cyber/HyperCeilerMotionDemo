package me.rain.kernel;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

/** Temporary local-only card key gate; intentionally performs no server validation. */
public final class CardKeyActivity extends ComponentActivity {
    private boolean navigating;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_key);
        configureWindow();
        findViewById(R.id.auth_back).setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.auth_confirm).setOnClickListener(view -> verify());
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        window.setDecorFitsSystemWindows(false);
        View root = findViewById(R.id.auth_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() |
                    WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void verify() {
        if (navigating) return;
        EditText input = findViewById(R.id.auth_key);
        String key = input.getText().toString().trim();
        if (key.isEmpty()) {
            input.setError(getString(R.string.auth_key_required));
            input.requestFocus();
            return;
        }
        navigating = true;
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
        try {
            // Future auth SDK replaces this branch. Do not persist or log the entered key.
            Intent home = new Intent(this, HomeActivity.class);
            home.putExtra(HomeActivity.SPRING_ENTRY, true);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(home);
            overridePendingTransition(0, 0);
        } catch (RuntimeException error) {
            navigating = false;
            android.util.Log.e("RainKernel", "Could not open home", error);
            Toast.makeText(this, R.string.auth_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        navigating = false;
    }

    @Override public void finish() {
        super.finish();
        overridePendingTransition(R.anim.provision_slide_in_left,
                R.anim.provision_slide_out_right);
    }
}
