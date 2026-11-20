package com.flm.boost;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int SHIZUKU_REQUEST = 104;
    private static final String FLM = "com.imageline.FLM";
    private LinearLayout root, appList;
    private TextView status, memory;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> selected = new ArrayList<>();
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (code, result) -> {
        if (code == SHIZUKU_REQUEST) updateStatus(result == PackageManager.PERMISSION_GRANTED ? "Shizuku đã cấp quyền" : "Shizuku chưa được cấp quyền");
    };
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(9,12,20));
        getWindow().setNavigationBarColor(Color.rgb(9,12,20));
        root = new LinearLayout(this); root.setOrientation(1); root.setPadding(28,32,28,24); root.setBackgroundColor(Color.rgb(9,12,20));
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        title("FLM BOOST  V1.4", 28, 0xff43f5c4);
        title("Performance mode for FL Studio Mobile", 14, 0xffa4afc2);
        status = title("Shizuku: đang kiểm tra", 15, Color.WHITE);
        memory = title("RAM: đang đọc", 15, 0xffa4afc2);
        button("1. KẾT NỐI SHIZUKU", () -> connect());
        button("2. CHỌN ỨNG DỤNG NỀN", () -> loadApps());
        appList = new LinearLayout(this); appList.setOrientation(1); root.addView(appList);
        button("3. BOOST + MỞ FL STUDIO", () -> boostAndLaunch());
        button("CHỈ MỞ FL STUDIO", () -> launchFlm());
        title("Chỉ đóng ứng dụng bạn chọn. Không dừng ứng dụng hệ thống, Shizuku hay FL Studio. Không ép xung CPU hoặc cam kết 60 FPS.", 13, 0xffa4afc2);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        refreshMemory(); updateStatus("Kiểm tra quyền Shizuku");
    }
    private TextView title(String value, int sp, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(sp); v.setTextColor(color); v.setPadding(0, 12, 0, 12);
        if (sp >= 25) v.setTypeface(null, Typeface.BOLD);
        root.addView(v); return v;
    }
    private void button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.rgb(5,15,20)); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff43f5c4));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin=12; root.addView(b,p); b.setOnClickListener(v -> action.run());
    }
    private void updateStatus(String s) { status.setText(s); }
    private boolean hasShizuku() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Exception e) { return false; }
    }
    private void connect() {
        try {
            if (!Shizuku.pingBinder()) { updateStatus("Hãy mở ứng dụng Shizuku và khởi động dịch vụ trước"); return; }
            if (hasShizuku()) { updateStatus("Shizuku đã sẵn sàng"); return; }
            Shizuku.requestPermission(SHIZUKU_REQUEST);
        } catch (Exception e) { updateStatus("Lỗi Shizuku: " + e.getMessage()); }
    }
    private void refreshMemory() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); am.getMemoryInfo(info);
        long mb = 1024L*1024L;
        memory.setText("RAM trống: " + info.availMem/mb + " MB / " + info.totalMem/mb + " MB");
    }
    private void loadApps() {
        appList.removeAllViews(); selected.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        Collections.sort(apps, (a,b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        int count=0;
        for (ApplicationInfo a : apps) {
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0 || a.packageName.equals(getPackageName()) || a.packageName.equals(FLM) || a.packageName.startsWith("moe.shizuku")) continue;
            CheckBox box = new CheckBox(this); box.setText(a.loadLabel(pm) + " (" + a.packageName + ")"); box.setTextColor(Color.WHITE);
            box.setOnCheckedChangeListener((v,checked) -> { if (checked) selected.add(a.packageName); else selected.remove(a.packageName); });
            appList.addView(box); count++;
        }
        updateStatus("Đã tìm " + count + " ứng dụng người dùng. Chọn ứng dụng muốn đóng.");
    }
    private void boostAndLaunch() {
        if (!hasShizuku()) { updateStatus("Cần cấp quyền Shizuku trước khi Boost"); connect(); return; }
        final List<String> packages = new ArrayList<>(selected);
        updateStatus("Đang tối ưu " + packages.size() + " ứng dụng...");
        new Thread(() -> {
            int ok=0; String error="";
            for (String pkg : packages) {
                if (!pkg.matches("[a-zA-Z0-9_.]+") || pkg.equals(FLM) || pkg.equals(getPackageName())) continue;
                try {
                    // Shizuku runs the command as ADB shell; no root or CPU overclocking.
                    Process p = Shizuku.newProcess(new String[]{"sh", "-c", "am force-stop " + pkg}, null, null);
                    int code = p.waitFor(); if (code==0) ok++;
                    else { BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream())); String line=br.readLine(); if(line!=null) error=line; }
                    p.destroy();
                } catch (Exception e) { error=e.getMessage(); }
            }
            final int completed=ok; final String lastError=error;
            handler.post(() -> { refreshMemory(); updateStatus("Đã đóng " + completed + "/" + packages.size() + " ứng dụng" + (lastError.isEmpty()?"":" · "+lastError)); launchFlm(); });
        }).start();
    }
    private void launchFlm() {
        Intent i = getPackageManager().getLaunchIntentForPackage(FLM);
        if (i == null) { updateStatus("Không tìm thấy FL Studio Mobile ("+FLM+")"); return; }
        startActivity(i);
    }
    @Override protected void onDestroy() { Shizuku.removeRequestPermissionResultListener(permissionListener); super.onDestroy(); }
}
