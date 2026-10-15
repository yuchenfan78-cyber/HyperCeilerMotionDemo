package me.rain.motiondemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

final class MotionController {
    private static final TimeInterpolator SINE_OUT = t -> (float) Math.sin(t * Math.PI * 0.5);
    private static final TimeInterpolator CUBIC_OUT = t -> 1f - (float) Math.pow(1f - t, 3);

    private final View brand;
    private final View replayButton;
    private final List<Animator> running = new ArrayList<>();

    MotionController(View brand, View replayButton) {
        this.brand = brand;
        this.replayButton = replayButton;
        reset();
    }

    void replay() {
        cancel();
        reset();

        ObjectAnimator firstScale = scale(brand, 0.5f, 0.95f, 440L, SINE_OUT);
        ObjectAnimator secondScale = scale(brand, 0.95f, 1f, 700L, CUBIC_OUT);
        AnimatorSet logoScale = new AnimatorSet();
        logoScale.playSequentially(firstScale, secondScale);

        ObjectAnimator logoFade = ObjectAnimator.ofFloat(brand, View.ALPHA, 0f, 1f);
        logoFade.setStartDelay(60L);
        logoFade.setDuration(540L);
        logoFade.setInterpolator(CUBIC_OUT);

        ObjectAnimator buttonEntrance = ObjectAnimator.ofPropertyValuesHolder(
            replayButton,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1f)
        );
        buttonEntrance.setStartDelay(1340L);
        buttonEntrance.setDuration(450L);
        buttonEntrance.setInterpolator(CUBIC_OUT);
        buttonEntrance.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) replayButton.setEnabled(true);
            }
        });

        running.add(logoScale);
        running.add(logoFade);
        running.add(buttonEntrance);
        for (Animator animator : running) animator.start();
    }

    void cancel() {
        for (Animator animator : running) animator.cancel();
        running.clear();
    }

    private void reset() {
        brand.setAlpha(0f);
        brand.setScaleX(0.5f);
        brand.setScaleY(0.5f);
        replayButton.setAlpha(0f);
        replayButton.setScaleX(0.9f);
        replayButton.setScaleY(0.9f);
        replayButton.setEnabled(false);
    }

    private static ObjectAnimator scale(View target, float from, float to,
                                        long duration, TimeInterpolator interpolator) {
        ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat(View.SCALE_X, from, to),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, from, to)
        );
        animation.setDuration(duration);
        animation.setInterpolator(interpolator);
        return animation;
    }
}
