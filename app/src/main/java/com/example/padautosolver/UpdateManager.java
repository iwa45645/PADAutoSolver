package com.example.padautosolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateManager {
    public static final int REQ_UNKNOWN_APPS = 2101;

    private final Activity activity;
    private volatile File pendingApk;
    private final AtomicBoolean checking = new AtomicBoolean();
    private final AtomicBoolean downloading = new AtomicBoolean();

    public UpdateManager(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdates(boolean userInitiated) {
        if (!checking.compareAndSet(false, true)) return;
        if (userInitiated) Toast.makeText(activity, "更新を確認しています…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String repo = BuildConfig.GITHUB_REPOSITORY;
                URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "PADAutoSolver/" + BuildConfig.VERSION_NAME);

                int code = conn.getResponseCode();
                if (code == 404) {
                    throw new IllegalStateException("GitHub Release が見つかりません。リポジトリが非公開の場合、自動更新には公開リポジトリが必要です。");
                }
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("GitHub API エラー: HTTP " + code);
                }

                StringBuilder json = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) json.append(line);
                }

                JSONObject release = new JSONObject(json.toString());
                String tag = release.optString("tag_name", "");
                String latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) <= 0) {
                    if (userInitiated) {
                        activity.runOnUiThread(() -> Toast.makeText(activity,
                                "最新版です（" + BuildConfig.VERSION_NAME + "）", Toast.LENGTH_LONG).show());
                    }
                    return;
                }

                JSONArray assets = release.optJSONArray("assets");
                String apkUrl = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
                        if (name.equals("padautosolver-release.apk")) {
                            apkUrl = asset.optString("browser_download_url", null);
                            if (name.contains("release")) break;
                        }
                    }
                }
                if (apkUrl == null || apkUrl.isEmpty()) {
                    throw new IllegalStateException("最新ReleaseにAPKファイルがありません。");
                }
                if (!apkUrl.startsWith("https://github.com/" + repo + "/releases/download/")) {
                    throw new IllegalStateException("更新APKの配布元が一致しません");
                }

                final String version = latestVersion;
                final String downloadUrl = apkUrl;
                activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                        .setTitle("新しいバージョンがあります")
                        .setMessage("現在: " + BuildConfig.VERSION_NAME + "\n新しい版: " + version + "\n\nAPKをダウンロードして更新しますか？")
                        .setNegativeButton("後で", null)
                        .setPositiveButton("更新", (d, w) -> downloadAndInstall(downloadUrl, version))
                        .show());
            } catch (Exception e) {
                if (userInitiated) {
                    String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    activity.runOnUiThread(() -> Toast.makeText(activity, "更新確認に失敗: " + msg, Toast.LENGTH_LONG).show());
                }
            } finally {
                if (conn != null) conn.disconnect();
                checking.set(false);
            }
        }, "pad-update-check").start();
    }

    private void downloadAndInstall(String downloadUrl, String version) {
        if (!downloading.compareAndSet(false, true)) return;
        Toast.makeText(activity, "v" + version + " をダウンロードしています…", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) throw new IllegalStateException("ダウンロード先を作成できません");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("ダウンロード先を作成できません");

                File out = new File(dir, "PADAutoSolver-update.apk.part");
                URL url = new URL(downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "PADAutoSolver/" + BuildConfig.VERSION_NAME);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("APK取得エラー: HTTP " + code);

                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buffer = new byte[32 * 1024];
                    int n;
                    while ((n = in.read(buffer)) >= 0) fos.write(buffer, 0, n);
                }
                verifyUpdateApk(out);
                File complete = new File(dir, "PADAutoSolver-update.apk");
                if (complete.exists() && !complete.delete()) throw new IllegalStateException("旧更新ファイルを削除できません");
                if (!out.renameTo(complete)) throw new IllegalStateException("更新ファイルを保存できません");
                pendingApk = complete;
                activity.runOnUiThread(this::requestInstallPermissionOrInstall);
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                activity.runOnUiThread(() -> Toast.makeText(activity, "更新APKの取得に失敗: " + msg, Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) conn.disconnect();
                downloading.set(false);
            }
        }, "pad-update-download").start();
    }

    @SuppressWarnings("deprecation")
    private void verifyUpdateApk(File file) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flags);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName)) {
            throw new IllegalStateException("更新APKのパッケージ名が一致しません");
        }
        long nextCode = Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode;
        long currentCode = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
        if (nextCode <= currentCode) throw new IllegalStateException("更新APKのversionCodeが増えていません");
        Signature[] next = Build.VERSION.SDK_INT >= 28
                ? (candidate.signingInfo == null ? null : candidate.signingInfo.getApkContentsSigners()) : candidate.signatures;
        Signature[] current = Build.VERSION.SDK_INT >= 28
                ? (installed.signingInfo == null ? null : installed.signingInfo.getApkContentsSigners()) : installed.signatures;
        if (next == null || current == null || !Arrays.equals(next, current)) {
            throw new IllegalStateException("更新APKの署名が一致しません");
        }
    }

    public void resumePendingInstall() {
        if (pendingApk != null && pendingApk.exists()
                && activity.getPackageManager().canRequestPackageInstalls()) requestInstallPermissionOrInstall();
    }

    private void requestInstallPermissionOrInstall() {
        if (pendingApk == null || !pendingApk.exists()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(settings, REQ_UNKNOWN_APPS);
            Toast.makeText(activity, "「この提供元のアプリを許可」を有効にしてください", Toast.LENGTH_LONG).show();
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                pendingApk);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(install);
            pendingApk = null;
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, "インストール画面を開けません: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static int compareVersions(String a, String b) {
        int[] aa = parseVersion(a);
        int[] bb = parseVersion(b);
        int n = Math.max(aa.length, bb.length);
        for (int i = 0; i < n; i++) {
            int av = i < aa.length ? aa[i] : 0;
            int bv = i < bb.length ? bb[i] : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int[] parseVersion(String s) {
        String clean = s == null ? "" : s.trim().replaceFirst("^[vV]", "");
        String[] parts = clean.split("[.-]");
        int[] out = new int[Math.min(parts.length, 4)];
        for (int i = 0; i < out.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].replaceAll("[^0-9].*$", ""));
            } catch (Exception ignored) {
                out[i] = 0;
            }
        }
        return out;
    }
}
