package me.rain.kernel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.security.auth.x500.X500Principal;

/**
 * Edits only the binary AndroidManifest.xml of this application's installed APK.
 * No source rebuild or Gradle task is involved. Changed APKs must be signed again.
 */
final class ApkPackageEditor {
    private static final String KEY_ALIAS = "rainkernel_local_clone_signer";
    private static final int MAX_MANIFEST = 4 * 1024 * 1024;
    private static final int MAX_ENTRIES = 30000;

    static final class Result {
        final File signedApk;
        final String packageName;

        Result(File signedApk, String packageName) {
            this.signedApk = signedApk;
            this.packageName = packageName;
        }
    }

    private ApkPackageEditor() { }

    static Result create(Context context, String destinationPackage) throws Exception {
        String original = context.getPackageName();
        if (!destinationPackage.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) {
            throw new IllegalArgumentException("包名格式不正确");
        }
        if (original.equals(destinationPackage)) {
            throw new IllegalArgumentException("新包名与当前安装包名相同");
        }
        // Uses the currently installed APK, not an unverified file from shared storage.
        File source = new File(context.getPackageCodePath());
        if (!source.isFile()) throw new IOException("当前 APK 不可读取");
        File outputDir = new File(context.getCacheDir(), "local-clone");
        if (!outputDir.exists() && !outputDir.mkdirs()) throw new IOException("无法创建临时目录");
        File unsigned = new File(outputDir, "unsigned.apk");
        File signed = new File(outputDir, "rainkernel-" + destinationPackage + ".apk");
        if (unsigned.exists() && !unsigned.delete()) throw new IOException("临时文件正在使用");
        if (signed.exists() && !signed.delete()) throw new IOException("旧安装包正在使用");
        try {
            rewriteApk(source, unsigned, original, destinationPackage);
            sign(unsigned, signed);
            PackageInfo info = context.getPackageManager().getPackageArchiveInfo(signed.getAbsolutePath(), 0);
            if (info == null || !destinationPackage.equals(info.packageName)) {
                throw new IOException("生成的 APK 包名校验失败");
            }
            ApkVerifier.Result verification = new ApkVerifier.Builder(signed).build().verify();
            if (!verification.isVerified()) throw new IOException("APK 签名校验失败");
            return new Result(signed, destinationPackage);
        } finally {
            unsigned.delete();
        }
    }

    private static void rewriteApk(File source, File unsigned, String oldPkg, String newPkg)
            throws IOException {
        try (ZipFile zip = new ZipFile(source);
             FileOutputStream file = new FileOutputStream(unsigned);
             CountingStream counter = new CountingStream(file);
             ZipOutputStream output = new ZipOutputStream(counter)) {
            int count = 0;
            boolean manifestWritten = false;
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (++count > MAX_ENTRIES) throw new IOException("APK 文件条目数量异常");
                ZipEntry input = entries.nextElement();
                if (input.isDirectory() || signatureFile(input.getName())) continue;
                boolean manifest = "AndroidManifest.xml".equals(input.getName());
                boolean resourceTable = "resources.arsc".equals(input.getName());
                byte[] data = null;
                if (manifest) {
                    if (input.getSize() < 1 || input.getSize() > MAX_MANIFEST)
                        throw new IOException("APK Manifest 大小异常");
                    try (InputStream stream = zip.getInputStream(input)) {
                        data = readLimited(stream, MAX_MANIFEST);
                    }
                    data = rewriteManifest(data, oldPkg, newPkg);
                    manifestWritten = true;
                }
                ZipEntry next = new ZipEntry(input.getName());
                next.setTime(input.getTime());
                // Android requires resources.arsc to be stored and 4-byte aligned.
                // Native libraries are compressed in this clone; the source manifest
                // explicitly opts into extractNativeLibs=true.
                if (resourceTable) {
                    next.setMethod(ZipEntry.STORED);
                    next.setSize(input.getSize());
                    next.setCrc(input.getCrc());
                    next.setExtra(alignmentExtra(counter.bytes, next.getName(), 4));
                } else {
                    next.setMethod(ZipEntry.DEFLATED);
                }
                output.putNextEntry(next);
                if (manifest) {
                    output.write(data);
                } else {
                    try (InputStream stream = zip.getInputStream(input)) {
                        stream.transferTo(output);
                    }
                }
                output.closeEntry();
            }
            if (!manifestWritten) throw new IOException("APK 缺少 AndroidManifest.xml");
        }
    }

    private static boolean signatureFile(String filename) {
        if (!filename.regionMatches(true, 0, "META-INF/", 0, 9)) return false;
        String name = filename.substring(9).toUpperCase(java.util.Locale.ROOT);
        return name.equals("MANIFEST.MF") || name.endsWith(".SF") || name.endsWith(".RSA")
                || name.endsWith(".DSA") || name.endsWith(".EC");
    }

    private static byte[] alignmentExtra(long offset, String name, int alignment) {
        long headerEnd = offset + 30 + name.getBytes(StandardCharsets.UTF_8).length;
        int padding = (int) ((alignment - ((headerEnd + 4) % alignment)) % alignment);
        byte[] extra = new byte[4 + padding];
        extra[0] = (byte) 0x1e;
        extra[1] = (byte) 0xa1;
        extra[2] = (byte) padding;
        extra[3] = (byte) (padding >>> 8);
        return extra;
    }

    private static void sign(File input, File output) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(3072)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSubject(new X500Principal("CN=RainKernel Local Clone"))
                    .setCertificateSerialNumber(BigInteger.ONE)
                    .build());
            generator.generateKeyPair();
            store.load(null);
        }
        PrivateKey privateKey = (PrivateKey) store.getKey(KEY_ALIAS, null);
        X509Certificate certificate = (X509Certificate) store.getCertificate(KEY_ALIAS);
        if (privateKey == null || certificate == null) throw new IOException("无法读取本地签名密钥");
        ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                "RainKernel", privateKey, Collections.singletonList(certificate)).build();
        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(input)
                .setOutputApk(output)
                .setMinSdkVersion(35)
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign();
    }

    private static byte[] readLimited(InputStream input, int max) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] block = new byte[16 * 1024];
        int n;
        while ((n = input.read(block)) != -1) {
            if (output.size() + n > max) throw new IOException("Manifest 超过最大限制");
            output.write(block, 0, n);
        }
        return output.toByteArray();
    }

    /** Rewrites a string pool entry without modifying resource or attribute indices. */
    private static byte[] rewriteManifest(byte[] xml, String from, String to) throws IOException {
        if (xml.length < 8 || u16(xml, 0) != 3 || u32(xml, 4) != xml.length)
            throw new IOException("APK Manifest 不是有效的二进制 XML");
        int at = 8;
        while (at + 8 <= xml.length) {
            int type = u16(xml, at);
            int header = u16(xml, at + 2);
            int size = u32(xml, at + 4);
            if (size < header || header < 8 || (long) at + size > xml.length)
                throw new IOException("Manifest XML 块结构损坏");
            if (type == 1) {
                byte[] pool = editStringPool(xml, at, size, from, to);
                if (pool == null) throw new IOException("Manifest 不包含当前包名，拒绝修改");
                ByteArrayOutputStream result = new ByteArrayOutputStream(xml.length + to.length());
                result.write(xml, 0, at);
                result.write(pool);
                result.write(xml, at + size, xml.length - at - size);
                byte[] changed = result.toByteArray();
                put32(changed, 4, changed.length);
                return changed;
            }
            at += size;
        }
        throw new IOException("Manifest 字符串表不存在");
    }

    private static byte[] editStringPool(byte[] xml, int pos, int size, String from, String to)
            throws IOException {
        if (u16(xml, pos + 2) != 28) throw new IOException("不支持的 XML 字符串表格式");
        int count = u32(xml, pos + 8);
        int styleCount = u32(xml, pos + 12);
        int flags = u32(xml, pos + 16);
        int start = u32(xml, pos + 20);
        int styleStart = u32(xml, pos + 24);
        if (count <= 0 || count > 200000 || styleCount < 0 || styleCount > 200000
                || 28L + 4L * (count + styleCount) > start || start >= size
                || (styleCount != 0 && (styleStart < start || styleStart > size)))
            throw new IOException("XML 字符串表边界非法");
        boolean utf8 = (flags & 0x100) != 0;
        int stringsEnd = styleStart != 0 ? styleStart : size;
        int tableEnd = 28 + (count + styleCount) * 4;
        ByteArrayOutputStream strings = new ByteArrayOutputStream();
        int[] offsets = new int[count];
        Map<Integer, Integer> previouslyWritten = new HashMap<>();
        boolean found = false;
        for (int i = 0; i < count; i++) {
            int oldOffset = u32(xml, pos + 28 + 4 * i);
            if (oldOffset < 0 || oldOffset >= stringsEnd - start)
                throw new IOException("字符串地址越界");
            Integer reused = previouslyWritten.get(oldOffset);
            if (reused != null) {
                offsets[i] = reused;
                continue;
            }
            int absolute = pos + start + oldOffset;
            int[] decoded = utf8 ? decodeUtf8(xml, absolute) : decodeUtf16(xml, absolute);
            int end = decoded[0];
            if (end > pos + stringsEnd) throw new IOException("字符串越界");
            String value = new String(xml, decoded[1], decoded[2],
                    utf8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16LE);
            offsets[i] = strings.size();
            previouslyWritten.put(oldOffset, offsets[i]);
            if (value.equals(from)) {
                found = true;
                writeEncodedString(strings, to, utf8);
            } else {
                strings.write(xml, absolute, end - absolute);
            }
        }
        if (!found) return null;
        while (((start + strings.size()) & 3) != 0) strings.write(0);
        ByteArrayOutputStream output = new ByteArrayOutputStream(size + to.length());
        output.write(xml, pos, 28);
        for (int offset : offsets) writeInt(output, offset);
        output.write(xml, pos + 28 + 4 * count, start - (28 + 4 * count));
        output.write(strings.toByteArray());
        if (styleStart != 0) output.write(xml, pos + styleStart, size - styleStart);
        byte[] pool = output.toByteArray();
        put32(pool, 4, pool.length);
        if (styleStart != 0) put32(pool, 24, start + strings.size());
        return pool;
    }

    private static int[] decodeUtf8(byte[] data, int from) throws IOException {
        int[] next = {from};
        readLen8(data, next);
        int bytes = readLen8(data, next);
        if (bytes < 0 || (long) next[0] + bytes >= data.length || data[next[0] + bytes] != 0)
            throw new IOException("非法 UTF8 字符串");
        int start = next[0];
        return new int[]{start + bytes + 1, start, bytes};
    }

    private static int[] decodeUtf16(byte[] data, int from) throws IOException {
        int[] next = {from};
        int chars = readLen16(data, next);
        if (chars < 0 || (long) next[0] + (long) chars * 2 + 1 >= data.length)
            throw new IOException("非法 UTF16 字符串");
        int start = next[0];
        int end = start + chars * 2;
        if (u16(data, end) != 0) throw new IOException("UTF16 字符串缺少结束符");
        return new int[]{end + 2, start, chars * 2};
    }

    private static int readLen8(byte[] bytes, int[] at) throws IOException {
        if (at[0] >= bytes.length) throw new IOException("UTF8 长度损坏");
        int first = bytes[at[0]++] & 0xff;
        if ((first & 0x80) == 0) return first;
        if (at[0] >= bytes.length) throw new IOException("UTF8 长度损坏");
        return ((first & 0x7f) << 8) | (bytes[at[0]++] & 0xff);
    }

    private static int readLen16(byte[] bytes, int[] at) throws IOException {
        int first = u16(bytes, at[0]);
        at[0] += 2;
        if ((first & 0x8000) == 0) return first;
        int second = u16(bytes, at[0]);
        at[0] += 2;
        return ((first & 0x7fff) << 16) | second;
    }

    private static void writeEncodedString(ByteArrayOutputStream out, String value, boolean utf8) {
        if (utf8) {
            byte[] raw = value.getBytes(StandardCharsets.UTF_8);
            writeLen8(out, value.length());
            writeLen8(out, raw.length);
            out.write(raw, 0, raw.length);
            out.write(0);
        } else {
            byte[] raw = value.getBytes(StandardCharsets.UTF_16LE);
            writeLen16(out, value.length());
            out.write(raw, 0, raw.length);
            out.write(0);
            out.write(0);
        }
    }

    private static void writeLen8(ByteArrayOutputStream out, int value) {
        if (value <= 127) out.write(value);
        else { out.write((value >>> 8) | 0x80); out.write(value & 255); }
    }

    private static void writeLen16(ByteArrayOutputStream out, int value) {
        if (value <= 32767) { out.write(value & 255); out.write((value >>> 8) & 255); }
        else {
            writeShort(out, (value >>> 16) | 0x8000);
            writeShort(out, value & 65535);
        }
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write(value & 255);
        out.write((value >>> 8) & 255);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        for (int i = 0; i < 4; i++) out.write((value >>> (8 * i)) & 255);
    }

    private static int u16(byte[] bytes, int at) throws IOException {
        if (at < 0 || (long) at + 2 > bytes.length) throw new IOException("读取 XML 时越界");
        return (bytes[at] & 255) | ((bytes[at + 1] & 255) << 8);
    }

    private static int u32(byte[] bytes, int at) throws IOException {
        if (at < 0 || (long) at + 4 > bytes.length) throw new IOException("读取 XML 时越界");
        return (bytes[at] & 255) | ((bytes[at + 1] & 255) << 8)
                | ((bytes[at + 2] & 255) << 16) | ((bytes[at + 3] & 255) << 24);
    }

    private static void put32(byte[] bytes, int at, int value) {
        for (int i = 0; i < 4; i++) bytes[at + i] = (byte) (value >>> (8 * i));
    }

    private static final class CountingStream extends OutputStream {
        private final OutputStream delegate;
        long bytes;

        CountingStream(OutputStream delegate) { this.delegate = delegate; }
        @Override public void write(int value) throws IOException { delegate.write(value); bytes++; }
        @Override public void write(byte[] data, int off, int len) throws IOException {
            delegate.write(data, off, len);
            bytes += len;
        }
        @Override public void flush() throws IOException { delegate.flush(); }
        @Override public void close() throws IOException { delegate.close(); }
    }
}
