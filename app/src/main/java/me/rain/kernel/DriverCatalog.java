package me.rain.kernel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Read-only index of the exact three JSON files supplied with RainKernel. */
final class DriverCatalog {
    private static final String[] FILES = {"LinYu.json", "Paradise.json", "Rt.json"};
    private static final String[] NAMES = {"LinYu", "Paradise", "Rt"};

    private DriverCatalog() { }

    static void bind(Activity activity, LinearLayout host) {
        host.removeAllViews();
        String kernel = System.getProperty("os.version", "");
        for (int group = 0; group < FILES.length; group++) {
            try (InputStream input = activity.getAssets().open("drivers/" + FILES[group])) {
                byte[] contents = input.readAllBytes();
                JSONArray entries = new JSONArray(new String(contents, StandardCharsets.UTF_8));
                section(activity, host, NAMES[group] + "  ·  " + entries.length());
                for (int i = 0; i < entries.length(); i++) {
                    String url = entries.getString(i);
                    // The JSON is an index, not executable code. Never download/execute it here.
                    String leaf = url.substring(url.lastIndexOf('/') + 1);
                    String label = leaf.endsWith(".sh") ? leaf.substring(0, leaf.length() - 3) : leaf;
                    boolean likelyMatch = group == 1 ? kernel.startsWith(label + ".") || kernel.equals(label)
                            : group == 2 && (kernel.startsWith(label + ".") || kernel.equals(label));
                    addRow(activity, host, label, likelyMatch ? "可能匹配当前内核 · 点击查看脚本来源" : "点击查看脚本来源", url);
                }
            } catch (Exception error) {
                section(activity, host, NAMES[group]);
                addRow(activity, host, "列表不可用", "无法读取本地 " + FILES[group], null);
            }
        }
    }

    private static void section(Activity activity, LinearLayout host, String title) {
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextColor(activity.getColor(R.color.text_primary));
        heading.setTextSize(18f);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(dp(activity, 6), dp(activity, 20), 0, dp(activity, 10));
        host.addView(heading);
    }

    private static void addRow(Activity activity, LinearLayout host,
                               String title, String subtitle, String url) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 18), dp(activity, 14), dp(activity, 18), dp(activity, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activity.getColor(R.color.surface_card));
        bg.setCornerRadius(dp(activity, 17));
        card.setBackground(bg);
        TextView name = new TextView(activity);
        name.setText(title);
        name.setTextColor(activity.getColor(R.color.text_primary));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setTextSize(15f);
        card.addView(name);
        TextView description = new TextView(activity);
        description.setText(subtitle);
        description.setTextColor(activity.getColor(R.color.text_secondary));
        description.setTextSize(12f);
        description.setPadding(0, dp(activity, 5), 0, 0);
        card.addView(description);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(activity, 8);
        host.addView(card, params);
        if (url == null) return;
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(activity.getString(R.string.drivers_address_notice) + "\n\n" + url)
                .setPositiveButton(R.string.drivers_copy_address, (d, which) -> {
                    ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (manager != null) manager.setPrimaryClip(ClipData.newPlainText("driver url", url));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }

    private static int dp(Context context, float count) {
        return Math.round(count * context.getResources().getDisplayMetrics().density);
    }
}
