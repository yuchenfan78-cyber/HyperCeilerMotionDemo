package me.rain.kernel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

/** Settings on the third tab. All work requiring Root happens asynchronously. */
final class ProfileSettingsController {
    private final HomeActivity activity;
    private final View root;
    private final SharedPreferences prefs;
    private final WeChatProfileReader reader = new WeChatProfileReader();
    private boolean avatarLoaded;
    private boolean reading;
    private TextView packageValue;
    private TextView themeValue;
    private ImageView avatar;
    private TextView nickname;
    private static final String[] THEME_LABELS = {
            "跟随系统", "白天模式", "夜间模式", "跟随时间调整"};
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ExecutorService packageWorker = Executors.newSingleThreadExecutor();
    private boolean editingPackage;

    ProfileSettingsController(HomeActivity activity, View root) {
        this.activity = activity;
        this.root = root;
        prefs = activity.getSharedPreferences(ThemePreferences.PREFS, Context.MODE_PRIVATE);
        avatar = root.findViewById(R.id.me_avatar);
        nickname = root.findViewById(R.id.me_nickname);
        build();
    }

    void loadAvatarOnce() {
        if (avatarLoaded) return;
        avatarLoaded = true;
        refreshWeChat();
    }

    void close() {
        reader.shutdown();
        if (editingPackage) packageWorker.shutdown();
        else packageWorker.shutdownNow();
    }

    boolean isEditingPackage() { return editingPackage; }

    private void build() {
        LinearLayout program = root.findViewById(R.id.me_program_group);
        addRow(program, activity.getString(R.string.me_permissions),
                "查看系统授权信息", () -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + activity.getPackageName()));
                    try { activity.startActivity(intent); }
                    catch (RuntimeException ignored) { toast("无法打开应用详情"); }
                });
        addRow(program, activity.getString(R.string.me_refresh_wechat),
                "需要 Root 授权 · 仅从本机读取", this::refreshWeChat);

        LinearLayout system = root.findViewById(R.id.me_system_group);
        addRow(system, activity.getString(R.string.me_kernel_version),
                System.getProperty("os.version", "未知"), null);
        addRow(system, activity.getString(R.string.me_sdk_version),
                String.valueOf(Build.VERSION.SDK_INT), null);
        addRow(system, activity.getString(R.string.me_device_model),
                Build.MANUFACTURER + " " + Build.MODEL, null);
        addRow(system, activity.getString(R.string.me_cpu_arch),
                String.join(" / ", Build.SUPPORTED_ABIS), null);
        String soc = Build.SOC_MODEL;
        if (soc == null || soc.isBlank() || Build.UNKNOWN.equalsIgnoreCase(soc))
            soc = Build.HARDWARE;
        addRow(system, activity.getString(R.string.me_processor), soc, null);

        LinearLayout assist = root.findViewById(R.id.me_assist_group);
        addToggle(assist, activity.getString(R.string.me_volume_shortcut),
                activity.getString(R.string.me_volume_explain), "volume_shortcut");
        addToggle(assist, activity.getString(R.string.me_no_background),
                activity.getString(R.string.me_no_background_explain), "no_background");

        LinearLayout packages = root.findViewById(R.id.me_package_group);
        addRow(packages, activity.getString(R.string.me_package_current),
                activity.getPackageName(), null);
        packageValue = addRow(packages, activity.getString(R.string.me_package_target),
                prefs.getString("package_target", activity.getPackageName()), this::choosePackage);
        addRow(packages, activity.getString(R.string.me_package_install),
                "直接修改当前 APK 包名并在手机上重新签名、Root 安装", this::confirmLocalClone);

        LinearLayout appearance = root.findViewById(R.id.me_appearance_group);
        themeValue = addRow(appearance, activity.getString(R.string.me_appearance_mode),
                themeLabel(), this::chooseTheme);

        LinearLayout about = root.findViewById(R.id.me_about_group);
        addRow(about, activity.getString(R.string.me_author),
                activity.getString(R.string.me_author_value), null);
        addRow(about, activity.getString(R.string.me_qq),
                "点击打开或填写群聊链接", () -> groupLink("qq_url", "QQ 群聊"));
        addRow(about, activity.getString(R.string.me_telegram),
                "点击打开或填写群聊链接", () -> groupLink("tg_url", "Telegram 群聊"));
    }

    private TextView addRow(LinearLayout group, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.setMinimumHeight(dp(66));
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextSize(15f);
        heading.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        heading.setTextColor(activity.getColor(R.color.text_primary));
        row.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        TextView details = new TextView(activity);
        details.setText(subtitle);
        details.setTextSize(12f);
        details.setTextColor(activity.getColor(R.color.text_secondary));
        details.setMaxLines(3);
        LinearLayout.LayoutParams note = new LinearLayout.LayoutParams(-1, -2);
        note.topMargin = dp(5);
        row.addView(details, note);
        group.addView(row, new LinearLayout.LayoutParams(-1, -2));
        if (action != null) {
            row.setClickable(true);
            row.setFocusable(true);
            android.util.TypedValue attribute = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                    attribute, true)) row.setForeground(activity.getDrawable(attribute.resourceId));
            row.setOnClickListener(v -> action.run());
        }
        return details;
    }

    private void addToggle(LinearLayout group, String title, String subtitle, String key) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(10), dp(14), dp(10));
        row.setMinimumHeight(dp(70));
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextSize(15f);
        heading.setTextColor(activity.getColor(R.color.text_primary));
        labels.addView(heading);
        TextView detail = new TextView(activity);
        detail.setText(subtitle);
        detail.setTextSize(12f);
        detail.setTextColor(activity.getColor(R.color.text_secondary));
        detail.setPadding(0, dp(4), 0, 0);
        labels.addView(detail);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch toggle = new Switch(activity);
        toggle.setChecked(prefs.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((button, enabled) ->
                prefs.edit().putBoolean(key, enabled).apply());
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-2, -2);
        toggleParams.leftMargin = dp(12);
        row.addView(toggle, toggleParams);
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        group.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void refreshWeChat() {
        if (reading) return;
        reading = true;
        reader.refresh(activity, avatar, nickname, () -> reading = false);
    }

    private void chooseTheme() {
        int index = 0;
        String saved = prefs.getString(ThemePreferences.MODE, "system");
        for (int i = 0; i < ThemePreferences.CHOICES.length; i++) {
            if (ThemePreferences.CHOICES[i].equals(saved)) index = i;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.me_appearance_mode)
                .setSingleChoiceItems(THEME_LABELS, index, (dialog, which) -> {
                    prefs.edit().putString(ThemePreferences.MODE,
                            ThemePreferences.CHOICES[which]).apply();
                    themeValue.setText(THEME_LABELS[which]);
                    dialog.dismiss();
                    activity.recreate();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private String themeLabel() {
        String mode = prefs.getString(ThemePreferences.MODE, "system");
        for (int i = 0; i < ThemePreferences.CHOICES.length; i++)
            if (ThemePreferences.CHOICES[i].equals(mode)) return THEME_LABELS[i];
        return THEME_LABELS[0];
    }

    private void choosePackage() {
        String[] choices = {"随机伪装包名", "指定包名"};
        new AlertDialog.Builder(activity).setTitle(R.string.me_package_target)
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        byte[] bytes = new byte[6];
                        RANDOM.nextBytes(bytes);
                        StringBuilder candidate = new StringBuilder("me.rain.rk");
                        for (byte value : bytes) candidate.append(String.format(Locale.ROOT, "%02x", value & 0xff));
                        savePackage(candidate.toString());
                    } else {
                        EditText input = new EditText(activity);
                        input.setSingleLine(true);
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        input.setHint(R.string.me_package_custom_hint);
                        input.setText(prefs.getString("package_target", ""));
                        LinearLayout holder = new LinearLayout(activity);
                        holder.setPadding(dp(22), 0, dp(22), 0);
                        holder.addView(input, new LinearLayout.LayoutParams(-1, -2));
                        new AlertDialog.Builder(activity).setTitle(R.string.me_package_custom)
                                .setView(holder)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, (d, w) ->
                                        savePackage(input.getText().toString().trim())).show();
                    }
                }).show();
    }

    private void savePackage(String value) {
        if (!value.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) {
            toast(activity.getString(R.string.me_package_invalid));
            return;
        }
        prefs.edit().putString("package_target", value).apply();
        packageValue.setText(value);
        toast(activity.getString(R.string.me_package_saved));
    }

    private void confirmLocalClone() {
        if (editingPackage) return;
        String target = prefs.getString("package_target", activity.getPackageName());
        if (target == null || !target.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) {
            toast("请先设置有效的新包名");
            return;
        }
        if (target.equals(activity.getPackageName())) {
            toast("新包名与当前包名相同");
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("本地 APK 包名伪装")
                .setMessage("新包名：" + target + "\n\n不运行 Gradle，不编译源码。修改本机 APK 的 Manifest、重新签名并通过 Root 安装。\n\n" +
                        "签名会变化，新包的数据与原应用隔离；部分依赖包名或原签名的功能可能无法在新包使用。建议先保留旧版验证。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("生成并安装", (dialog, which) -> createAndInstall(target))
                .show();
    }

    private void createAndInstall(String target) {
        if (editingPackage) return;
        editingPackage = true;
        toast("正在本机修改并签名 APK，请稍候");
        packageWorker.execute(() -> {
            ApkPackageEditor.Result result = null;
            String failure = null;
            try {
                result = ApkPackageEditor.create(activity, target);
                LocalCloneInstaller.install(result);
            } catch (Exception error) {
                failure = error.getMessage();
                android.util.Log.e("RainKernel", "本地包名伪装失败", error);
            } finally {
                if (result != null) result.signedApk.delete();
            }
            String finalFailure = failure;
            activity.runOnUiThread(() -> {
                editingPackage = false;
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (finalFailure != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle("包名伪装失败")
                            .setMessage(finalFailure)
                            .setPositiveButton(android.R.string.ok, null).show();
                } else {
                    offerOriginalRemoval(target);
                }
            });
        });
    }

    private void offerOriginalRemoval(String target) {
        final String original = activity.getPackageName();
        new AlertDialog.Builder(activity)
                .setTitle("新包安装成功")
                .setMessage("已由系统确认安装 " + target + "。\n\n建议先打开新包确认正常。" +
                        "如需删除旧版 " + original + "，旧版私有数据将被清除，且当前进程会结束。")
                .setNegativeButton("保留旧版", null)
                .setPositiveButton("删除旧版", (dialog, which) -> confirmOriginalRemoval(original, target))
                .show();
    }

    private void confirmOriginalRemoval(String original, String target) {
        new AlertDialog.Builder(activity)
                .setTitle("确认卸载旧版？")
                .setMessage("新安装：" + target + "\n即将卸载：" + original +
                        "\n\n这会删除旧版应用数据且不可恢复。请先确认新包能够正常运行。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("确定卸载", (dialog, which) -> packageWorker.execute(() -> {
                    try { LocalCloneInstaller.uninstallOriginal(original); }
                    catch (Exception error) { android.util.Log.e("RainKernel", "旧版卸载失败", error); }
                })).show();
    }

    private void groupLink(String key, String label) {
        String saved = prefs.getString(key, "");
        if (saved == null || saved.isBlank()) {
            editLink(key, label);
            return;
        }
        new AlertDialog.Builder(activity).setTitle(label)
                .setItems(new String[]{"打开群聊链接", "修改群聊链接"}, (d, option) -> {
                    if (option == 1) editLink(key, label);
                    else {
                        try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(saved))); }
                        catch (RuntimeException ignored) { toast("无法打开链接"); }
                    }
                }).show();
    }

    private void editLink(String key, String label) {
        EditText editor = new EditText(activity);
        editor.setSingleLine(true);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        editor.setHint(R.string.me_set_link_hint);
        editor.setText(prefs.getString(key, ""));
        LinearLayout holder = new LinearLayout(activity);
        holder.setPadding(dp(22), 0, dp(22), 0);
        holder.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(activity).setTitle(label).setView(holder)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    String url = editor.getText().toString().trim();
                    if (!url.isEmpty() && !url.startsWith("https://")) {
                        toast("请输入 https:// 开头的邀请链接");
                        return;
                    }
                    prefs.edit().putString(key, url).apply();
                }).show();
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float size) {
        return Math.round(size * activity.getResources().getDisplayMetrics().density);
    }
}
