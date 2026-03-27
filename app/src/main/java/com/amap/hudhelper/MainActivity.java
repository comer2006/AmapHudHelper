package com.amap.hudhelper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

import java.util.List;

/**
 * 高德HUD助手 - 主界面
 */
public class MainActivity extends Activity {
    
    private TextView statusText;
    private Button openSettingsBtn;
    private Button startServiceBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        openSettingsBtn = findViewById(R.id.openSettingsBtn);
        startServiceBtn = findViewById(R.id.startServiceBtn);
        
        // 检查服务状态
        checkServiceStatus();
        
        // 打开无障碍设置
        openSettingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }
    
    /**
     * 检查AccessibilityService状态
     */
    private void checkServiceStatus() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        );
        
        boolean isServiceEnabled = false;
        for (AccessibilityServiceInfo service : services) {
            if (service.getResolveInfo().serviceInfo.packageName.equals(getPackageName())) {
                isServiceEnabled = true;
                break;
            }
        }
        
        if (isServiceEnabled) {
            statusText.setText("✅ 服务已启用\n正在监控高德地图...");
            statusText.setTextColor(0xFF00FF00);
            startServiceBtn.setEnabled(false);
            startServiceBtn.setText("服务运行中");
        } else {
            statusText.setText("⚠️ 服务未启用\n请点击按钮开启无障碍服务");
            statusText.setTextColor(0xFFFFA500);
            startServiceBtn.setEnabled(true);
            startServiceBtn.setText("开启服务");
        }
    }
}
