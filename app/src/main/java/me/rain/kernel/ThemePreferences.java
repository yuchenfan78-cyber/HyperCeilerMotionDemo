package me.rain.kernel;

import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;

import java.util.Calendar;

final class ThemePreferences {
    static final String PREFS = "rainkernel";
    static final String MODE = "appearance_mode";
    static final String[] CHOICES = {"system", "light", "dark", "time"};

    private ThemePreferences() { }

    static Context wrap(Context original) {
        SharedPreferences prefs = original.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String mode = prefs.getString(MODE, "system");
        if ("system".equals(mode)) return original;
        boolean dark = "dark".equals(mode) || ("time".equals(mode) && isNightByClock());
        Configuration config = new Configuration(original.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return original.createConfigurationContext(config);
    }

    static boolean isNightByClock() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 6 || hour >= 18;
    }
}
