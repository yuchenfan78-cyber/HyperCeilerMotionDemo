package me.rain.kernel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Xml;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Reads the user's locally installed WeChat profile without copying it to shared storage. */
final class WeChatProfileReader {
    private static final String AVATAR_SCRIPT =
            "d='/data/user/0/com.tencent.mm/MicroMsg/last_avatar_dir'; " +
            "set -- \"$d\"/*.png; " +
            "if [ \"$#\" -eq 1 ] && [ -f \"$1\" ]; then cat \"$1\"; fi";
    private static final String NICK_SCRIPT =
            "cat '/data/user/0/com.tencent.mm/shared_prefs/com.tencent.mm_preferences.xml'";
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private volatile boolean closed;

    void refresh(Activity activity, ImageView avatar, TextView nickname, Runnable finished) {
        if (closed) return;
        worker.execute(() -> {
            String name = null;
            Bitmap icon = null;
            try { name = decodeNickname(rootRead(NICK_SCRIPT, 512 * 1024)); }
            catch (Exception ignored) { }
            try { icon = decodeAvatar(rootRead(AVATAR_SCRIPT, 5 * 1024 * 1024)); }
            catch (Exception ignored) { }
            final String resultName = name;
            final Bitmap resultIcon = icon;
            activity.runOnUiThread(() -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                if (resultName != null && !resultName.isBlank()) nickname.setText(resultName);
                if (resultIcon != null) avatar.setImageBitmap(resultIcon);
                finished.run();
            });
        });
    }

    private byte[] rootRead(String script, int limit) throws Exception {
        Process process = new ProcessBuilder("su", "-c", script).start();
        Future<byte[]> stream = worker.submit(() -> boundedRead(process.getInputStream(), limit));
        try {
            if (!process.waitFor(8, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                stream.cancel(true);
                return new byte[0];
            }
            return stream.get(2, TimeUnit.SECONDS);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static byte[] boundedRead(InputStream stream, int limit) throws Exception {
        try (InputStream input = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] block = new byte[16 * 1024];
            int count;
            while ((count = input.read(block)) != -1) {
                if (out.size() + count > limit) throw new IllegalArgumentException("Input too large");
                out.write(block, 0, count);
            }
            return out.toByteArray();
        }
    }

    private static String decodeNickname(byte[] xml) throws Exception {
        if (xml.length == 0) return null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(xml), StandardCharsets.UTF_8.name());
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "string".equals(parser.getName())
                    && "last_login_nick_name".equals(parser.getAttributeValue(null, "name"))) {
                String result = parser.nextText().trim();
                return result.length() <= 100 ? result : result.substring(0, 100);
            }
        }
        return null;
    }

    private static Bitmap decodeAvatar(byte[] raw) {
        if (raw.length < 24 || raw[0] != (byte) 0x89 || raw[1] != 0x50
                || raw[2] != 0x4e || raw[3] != 0x47) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
        if (bounds.outWidth < 1 || bounds.outHeight < 1) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth / options.inSampleSize,
                bounds.outHeight / options.inSampleSize) > 320) options.inSampleSize *= 2;
        return BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
    }

    void shutdown() {
        closed = true;
        worker.shutdownNow();
    }
}
