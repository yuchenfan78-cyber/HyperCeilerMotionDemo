package me.rain.motiondemo;

import android.view.View;

import java.lang.reflect.Method;

final class NativeBlur {
    private static final int[] LOGO_COLORS = {-867546550, -11579569, -15011328};
    private static final int[] LOGO_MODES = {19, 100, 106};
    private static final int[] BUTTON_COLORS = {-13750738, -15011328};
    private static final int[] BUTTON_MODES = {100, 106};

    private NativeBlur() { }

    static void setupContainer(View view, int radiusPx) {
        invoke(view, "setMiBackgroundBlurRadius", new Class<?>[]{int.class}, radiusPx);
        invoke(view, "setMiViewBlurMode", new Class<?>[]{int.class}, 0);
    }

    static boolean setupLogo(View view) {
        return setup(view, LOGO_COLORS, LOGO_MODES);
    }

    static boolean setupButton(View view) {
        return setup(view, BUTTON_COLORS, BUTTON_MODES);
    }

    private static boolean setup(View view, int[] colors, int[] modes) {
        if (!invoke(view, "setMiViewBlurMode", new Class<?>[]{int.class}, 3)) {
            return false;
        }
        for (int i = 0; i < colors.length; i++) {
            if (!invoke(view, "addMiBackgroundBlendColor",
                new Class<?>[]{int.class, int.class}, colors[i], modes[i])) {
                invoke(view, "setMiViewBlurMode", new Class<?>[]{int.class}, 0);
                return false;
            }
        }
        return true;
    }

    private static boolean invoke(View view, String name, Class<?>[] args, Object... values) {
        try {
            Method method = View.class.getMethod(name, args);
            method.invoke(view, values);
            return true;
        } catch (ReflectiveOperationException | SecurityException exception) {
            return false;
        }
    }
}
