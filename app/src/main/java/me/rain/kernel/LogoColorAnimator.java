package me.rain.kernel;

import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/** Drives both welcome marks through the glow's white / lavender / rose palette. */
final class LogoColorAnimator {
    private final ValueAnimator animator;

    LogoColorAnimator(ImageView icon, ImageView wordmark) {
        // High luminance maintains legibility over the moving shader; MIUI backdrop
        // blending is still applied separately by NativeBlur on supported devices.
        animator = ValueAnimator.ofArgb(
                0xFFFFFFFF, 0xFFF4E9FF, 0xFFE2DDFF,
                0xFFFFDDEB, 0xFFFFEEF5, 0xFFFFFFFF);
        animator.setDuration(12000L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(value -> {
            int tint = (Integer) value.getAnimatedValue();
            icon.setColorFilter(tint);
            wordmark.setColorFilter(tint);
        });
    }

    void startOrResume() {
        if (animator.isPaused()) animator.resume();
        else if (!animator.isStarted()) animator.start();
    }

    void pause() {
        if (animator.isRunning()) animator.pause();
    }

    void destroy() { animator.cancel(); }
}
