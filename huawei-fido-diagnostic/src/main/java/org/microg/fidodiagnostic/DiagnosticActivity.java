/*
 * SPDX-FileCopyrightText: 2026
 * SPDX-License-Identifier: Apache-2.0
 *
 * Offline, read-only diagnostic. Does not request INTERNET and does not touch
 * credentials, keys, accounts or the existing microG installation.
 */
package org.microg.fidodiagnostic;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public final class DiagnosticActivity extends Activity {
    private TextView results;
    private String lastReport = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int space = Math.round(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(space, space, space, space);

        TextView title = new TextView(this);
        title.setText("华为 FIDO2 / Google 通行密钥诊断");
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(title);

        TextView privacy = new TextView(this);
        privacy.setText("只读、离线：不读取通行密钥、账号或指纹数据，不修改 microG。");
        privacy.setTextSize(13);
        layout.addView(privacy);

        Button refresh = new Button(this);
        refresh.setText("重新检查");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { runDiagnostics(); }
        });
        layout.addView(refresh);

        Button copy = new Button(this);
        copy.setText("复制诊断结果");
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("FIDO2 diagnostics", lastReport));
                    android.widget.Toast.makeText(DiagnosticActivity.this, "已复制", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(copy);

        results = new TextView(this);
        results.setTypeface(Typeface.MONOSPACE);
        results.setTextSize(13);
        results.setTextIsSelectable(true);
        layout.addView(results);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
        runDiagnostics();
    }

    private void runDiagnostics() {
        StringBuilder b = new StringBuilder();
        line(b, "=== 系统 / 指纹 ===");
        line(b, "设备: " + Build.MANUFACTURER + " " + Build.MODEL);
        line(b, "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        line(b, "安全锁屏: " + secureLock());
        line(b, "系统指纹: " + fingerprintStatus());
        line(b, "");

        PackageManager pm = getPackageManager();
        line(b, "=== 已安装的 Google 组件 ===");
        packageStatus(b, pm, "Google 服务 / microG", "com.google.android.gms");
        packageStatus(b, pm, "microG user 模式", "org.microg.gms");
        packageStatus(b, pm, "Google 服务框架", "com.google.android.gsf");
        packageStatus(b, pm, "Google Play 商店", "com.android.vending");
        packageStatus(b, pm, "Chrome", "com.android.chrome");
        line(b, "");

        line(b, "=== FIDO2 Android Binder 服务（只检查可见性） ===");
        fidoServiceStatus(b, pm, "privileged / Chrome", "com.google.android.gms.fido.fido2.privileged.START");
        fidoServiceStatus(b, pm, "regular / App", "com.google.android.gms.fido.fido2.regular.START");
        line(b, "");

        line(b, "=== Google 通行密钥兼容性 ===");
        if (Build.VERSION.SDK_INT < 34) {
            line(b, "Android 第三方凭据提供器: 无（该系统接口从 Android 14 开始）");
            line(b, "Android 12 的 Chrome 通行密钥通常依赖 Google Play 服务中的 Google Password Manager。");
        } else {
            line(b, "Android 14+：需另外核实默认通行密钥提供器设置。");
        }
        line(b, "Google Password Manager 服务端资格: 无法通过 Android 公共 API 判定。");
        line(b, "FIDO2 服务可见性 != Google 账号通过本设备可创建通行密钥。");
        line(b, "");
        line(b, "本工具不检查 Google Play Integrity，也不能判断账户是否被 Google 允许注册。");
        line(b, "若 WebAuthn.io 注册/登录成功但 Google 本机选项灰色，优先检查 Google Password Manager 集成。");

        lastReport = b.toString();
        results.setText(lastReport);
    }

    private String secureLock() {
        KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return manager == null ? "不可用" : manager.isDeviceSecure() ? "已启用" : "未启用";
    }

    @SuppressWarnings("deprecation")
    private String fingerprintStatus() {
        try {
            FingerprintManager manager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (manager == null || !manager.isHardwareDetected()) return "Android API 未检测到指纹硬件";
            return manager.hasEnrolledFingerprints() ? "已录入" : "硬件存在，未检测到录入的指纹";
        } catch (SecurityException e) {
            return "系统拒绝查询";
        } catch (RuntimeException e) {
            return "查询失败：" + e.getClass().getSimpleName();
        }
    }

    @SuppressWarnings("deprecation")
    private static void packageStatus(StringBuilder b, PackageManager pm, String label, String name) {
        try {
            PackageInfo info = pm.getPackageInfo(name, 0);
            line(b, label + ": " + (info.versionName == null ? "版本未知" : info.versionName)
                + (info.applicationInfo != null && info.applicationInfo.enabled ? " [启用]" : " [禁用]"));
        } catch (PackageManager.NameNotFoundException e) {
            line(b, label + ": 未发现/不可见");
        } catch (RuntimeException e) {
            line(b, label + ": 查询失败：" + e.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("deprecation")
    private static void fidoServiceStatus(StringBuilder b, PackageManager pm, String label, String action) {
        Intent intent = new Intent(action);
        intent.setPackage("com.google.android.gms");
        try {
            List<ResolveInfo> services = pm.queryIntentServices(intent, 0);
            if (services == null || services.isEmpty()) {
                line(b, label + ": 未发现可解析服务");
                return;
            }
            for (ResolveInfo resolved : services) {
                if (resolved.serviceInfo == null) continue;
                line(b, label + ": " + resolved.serviceInfo.name
                    + " | exported=" + resolved.serviceInfo.exported
                    + " | enabled=" + resolved.serviceInfo.enabled);
            }
        } catch (RuntimeException e) {
            line(b, label + ": 查询失败：" + e.getClass().getSimpleName());
        }
    }

    private static void line(StringBuilder b, String s) {
        b.append(s).append('\n');
    }
}
