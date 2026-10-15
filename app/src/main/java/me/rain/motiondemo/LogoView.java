package me.rain.motiondemo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public final class LogoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean night;

    public LogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        night = (getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        RectF bounds = new RectF(left, top, left + size, top + size);

        int from = night ? Color.rgb(255, 252, 249) : Color.rgb(39, 37, 35);
        int to = night ? Color.rgb(216, 209, 204) : Color.rgb(72, 66, 62);
        paint.setShader(new LinearGradient(left, top, left + size, top + size,
            from, to, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, size * 0.26f, size * 0.26f, paint);
        paint.setShader(null);
        paint.setColor(night ? Color.rgb(45, 41, 39) : Color.WHITE);

        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(45f + i * 90f);
            float width = size * 0.075f;
            RectF petal = new RectF(-width, -size * 0.32f, width, -size * 0.105f);
            canvas.drawRoundRect(petal, width, width, paint);
            canvas.restore();
        }
        canvas.drawCircle(0, 0, size * 0.055f, paint);
        canvas.restore();
    }
}
