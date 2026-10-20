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
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import java.util.Locale;
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

        Button play = new Button(this);
        play.setText("打开 Google Play（查看设备认证）");
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openPlayStore(); }
        });
        layout.addView(play);

        Button passkeys = new Button(this);
        passkeys.setText("打开 Google 账号通行密钥");
        passkeys.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openWebPage("https://myaccount.google.com/signinoptions/passkeys");
            }
        });
        layout.addView(passkeys);

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

        line(b, "");
        line(b, "=== 系统列举的 Google / microG 相关安装包 ===");
        listRelatedPackages(b, pm);
        line(b, "");
        line(b, "=== FIDO2 Android Binder 服务（只检查可见性） ===");
        fidoServiceStatus(b, pm, "privileged / Chrome", "com.google.android.gms.fido.fido2.privileged.START");
        fidoServiceStatus(b, pm, "regular / App", "com.google.android.gms.fido.fido2.regular.START");
        inspectFidoServices(b, pm);
        line(b, "");

        line(b, "=== Google Play / Play Integrity 认证 ===");
        line(b, "Play Protect 设备认证: 无公开 API 可以可靠读取。");
        line(b, "官方检查: Play 商店 -> 头像 -> 设置 -> 关于 -> Play 保护机制认证。");
        line(b, "Google Play Integrity: 必须由注册的开发者应用请求并在服务端解密判定结果。");
        line(b, "包名和版本可见、Play 商店可打开、指纹正常均不代表设备已通过认证。");
        line(b, "此工具不伪造 Play Integrity 判定，也不绕过 Google 设备认证。");
        line(b, "Android 安全补丁日期: " + Build.VERSION.SECURITY_PATCH);
        line(b, "系统 Build 类型: " + Build.TYPE + " (仅供参考，不等同于认证)");
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

    private void openPlayStore() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.vending");
        if (launch == null) {
            android.widget.Toast.makeText(this, "Google Play 商店不可打开", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivity(launch);
            android.widget.Toast.makeText(this, "进入 Play 商店：头像 → 设置 → 关于 → 设备认证", android.widget.Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, "无法启动 Play 商店", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void openWebPage(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, "找不到可打开网页的浏览器", android.widget.Toast.LENGTH_LONG).show();
        }
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
            line(b, label + ": 此包名未找到（不能推断相关应用未安装）");
        } catch (RuntimeException e) {
            line(b, label + ": 查询失败：" + e.getClass().getSimpleName());
        }
    }


    /**
     * Enumerate relevant packages without assuming that vendor forks or
     * compatibility layers use com.google.android.gms/com.android.vending.
     * This app has no INTERNET permission; the list is shown only on-device.
     */
    @SuppressWarnings("deprecation")
    private static void listRelatedPackages(StringBuilder b, PackageManager pm) {
        try {
            List<PackageInfo> installed = pm.getInstalledPackages(0);
            int matches = 0;
            for (PackageInfo info : installed) {
                String id = info.packageName;
                String name = "";
                if (info.applicationInfo != null) {
                    try {
                        CharSequence label = pm.getApplicationLabel(info.applicationInfo);
                        name = label == null ? "" : label.toString();
                    } catch (RuntimeException ignored) {}
                }
                String check = (id + " " + name).toLowerCase(Locale.ROOT);
                if (!check.contains("microg") &&
                    !check.contains("mgoogle") &&
                    !check.contains("gmscore") &&
                    !check.contains("phonesky") &&
                    !check.contains("google play") &&
                    !check.contains("play store") &&
                    !check.contains("play 商店") &&
                    !check.contains("google 服务") &&
                    !check.contains("谷歌") &&
                    !check.contains("gservice") &&
                    !check.contains("gms") &&
                    !check.contains("vending") &&
                    !check.contains("gsf") &&
                    !check.equals("com.google.android.gms") &&
                    !check.contains("com.android.vending") &&
                    !check.contains("com.google.android.gsf")) continue;
                line(b, id + " | " + name + " | " + info.versionName);
                matches++;
                if (matches >= 40) {
                    line(b, "(相关软件包较多，前 40 项)");
                    break;
                }
            }
            line(b, "当前用户可见安装包: " + installed.size() + " | 相关匹配: " + matches);
            if (matches == 0) {
                line(b, "无匹配：可能使用未知包名、另一用户空间或系统阻止查询；不可判定未安装。");
            }
        } catch (RuntimeException e) {
            line(b, "查询相关安装包失败：" + e.getClass().getSimpleName());
        }
    }

    /**
     * A service may exist but not appear in queryIntentServices if its
     * exported intent filter differs or is disabled. Inspect declarations,
     * without binding to the service or accessing FIDO secrets.
     */
    @SuppressWarnings("deprecation")
    private static void inspectFidoServices(StringBuilder b, PackageManager pm) {
        String[] candidates = {
            "com.google.android.gms", "org.microg.gms",
            "com.mgoogle.android.gms", "app.revanced.android.gms"
        };
        int declarations = 0;
        for (String pkg : candidates) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg,
                    PackageManager.GET_SERVICES | PackageManager.MATCH_DISABLED_COMPONENTS);
                ServiceInfo[] services = info.services;
                if (services == null) continue;
                for (ServiceInfo service : services) {
                    String name = service.name == null ? "" : service.name;
                    if (!name.toLowerCase(Locale.ROOT).contains("fido")) continue;
                    line(b, "服务声明: " + pkg + " -> " + name
                        + " | exported=" + service.exported + " | enabled=" + service.enabled);
                    declarations++;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // A particular package ID is not present; other IDs may be.
            } catch (RuntimeException e) {
                line(b, pkg + " 服务检查失败: " + e.getClass().getSimpleName());
            }
        }
        if (declarations == 0) {
            line(b, "没有找到常见包名下的 FIDO 服务声明；不能直接证明 FIDO2 不工作。");
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
