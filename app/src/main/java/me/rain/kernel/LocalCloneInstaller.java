package me.rain.kernel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Root installation is deliberately separate from APK generation. */
final class LocalCloneInstaller {
    private LocalCloneInstaller() { }

    static void install(ApkPackageEditor.Result clone) throws Exception {
        File apk = clone.signedApk;
        if (!apk.isFile() || apk.length() <= 0) throw new IOException("安装 APK 文件不存在");
        // System package manager can't reliably access an application's private cache.
        String name = "/data/local/tmp/rainkernel-clone-" + System.nanoTime() + ".apk";
        String command = "cp " + quote(apk.getAbsolutePath()) + " " + quote(name)
                + " && chmod 0644 " + quote(name)
                + " && pm install -r --user 0 " + quote(name)
                + "; result=$?; rm -f " + quote(name) + "; exit $result";
        String result = execute(command, 120);
        if (!result.contains("Success")) throw new IOException("安装器没有返回成功状态：" + result);
        String verification = execute("pm path " + quote(clone.packageName), 15);
        if (!verification.contains("package:"))
            throw new IOException("Root 安装操作返回成功，但新包未通过系统核验");
    }

    static void uninstallOriginal(String originalName) throws Exception {
        if (!originalName.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+"))
            throw new IllegalArgumentException("旧包名无效");
        String result = execute("pm uninstall --user 0 " + quote(originalName), 40);
        if (!result.contains("Success")) throw new IOException("旧版卸载未完成：" + result);
    }

    private static String quote(String text) {
        return "'" + text.replace("'", "'\\''") + "'";
    }

    private static String execute(String command, int seconds) throws Exception {
        Process process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        try {
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Root 命令执行超时");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream input = process.getInputStream()) {
                byte[] block = new byte[4096];
                int n;
                while ((n = input.read(block)) != -1) {
                    if (buffer.size() + n > 64 * 1024) throw new IOException("Root 输出过长");
                    buffer.write(block, 0, n);
                }
            }
            String output = buffer.toString(StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) throw new IOException(output.isEmpty()
                    ? "Root 命令失败 (" + process.exitValue() + ")" : output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
