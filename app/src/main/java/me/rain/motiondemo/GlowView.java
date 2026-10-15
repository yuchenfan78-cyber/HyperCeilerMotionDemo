package me.rain.motiondemo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class GlowView extends View implements Choreographer.FrameCallback {
    private final RuntimeShader shader;
    private boolean running;
    private long startTimeNanos;

    public GlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        shader = new RuntimeShader(readShader(context));
        boolean night = (getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        shader.setFloatUniform("uDark", night ? 1f : 0f);
        shader.setFloatUniform("uResolution", 1f, 1f);
        shader.setFloatUniform("uTime", 0f);
        shader.setFloatUniform("uReveal", 0f);
        setBackgroundColor(context.getColor(R.color.surface));
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setRenderEffect(RenderEffect.createShaderEffect(shader));
    }

    public void replay() {
        startTimeNanos = System.nanoTime();
        renderFrame(0f);
        start();
    }

    public void start() {
        if (running) return;
        running = true;
        if (startTimeNanos == 0L) startTimeNanos = System.nanoTime();
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void stop() {
        if (!running) return;
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        float elapsed = Math.max(0f, (frameTimeNanos - startTimeNanos) / 1_000_000_000f);
        renderFrame(elapsed);
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void renderFrame(float elapsed) {
        shader.setFloatUniform("uTime", elapsed);
        shader.setFloatUniform("uReveal", Math.min(elapsed / 1.8f, 1f));
        setRenderEffect(RenderEffect.createShaderEffect(shader));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        shader.setFloatUniform("uResolution", Math.max(1, w), Math.max(1, h));
        setRenderEffect(RenderEffect.createShaderEffect(shader));
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private static String readShader(Context context) {
        StringBuilder source = new StringBuilder();
        try (InputStream stream = context.getResources().openRawResource(R.raw.glow);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) source.append(line).append('\n');
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load glow shader", exception);
        }
        return source.toString();
    }
}
