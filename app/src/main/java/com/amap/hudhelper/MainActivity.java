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
    
    private static final int REQUEST_AREA_SELECT = 1001;
    
    private TextView statusText;
    private Button openSettingsBtn;
    private Button startServiceBtn;
    private Button areaSelectBtn;
    private HudDisplayConfig config;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        config = HudDisplayConfig.getInstance(this);
        
        statusText = findViewById(R.id.statusText);
        openSettingsBtn = findViewById(R.id.openSettingsBtn);
        startServiceBtn = findViewById(R.id.startServiceBtn);
        areaSelectBtn = findViewById(R.id.areaSelectBtn);
        
        // 检查服务状态
        checkServiceStatus();
        
        // 打开无障碍设置
        openSettingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        
        // 区域选择按钮
        areaSelectBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HudAreaSelectActivity.class);
            startActivityForResult(intent, REQUEST_AREA_SELECT);
        });
        
        // 显示当前区域状态
        updateAreaStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AREA_SELECT && resultCode == RESULT_OK) {
            // 保存配置
            config.setHudX(data.getFloatExtra(HudAreaSelectActivity.EXTRA_HUD_X, config.getHudX()));
            config.setHudY(data.getFloatExtra(HudAreaSelectActivity.EXTRA_HUD_Y, config.getHudY()));
            config.setHudWidth(data.getFloatExtra(HudAreaSelectActivity.EXTRA_HUD_WIDTH, config.getHudWidth()));
            config.setHudHeight(data.getFloatExtra(HudAreaSelectActivity.EXTRA_HUD_HEIGHT, config.getHudHeight()));
            config.setLaneX(data.getFloatExtra(HudAreaSelectActivity.EXTRA_LANE_X, config.getLaneX()));
            config.setLaneY(data.getFloatExtra(HudAreaSelectActivity.EXTRA_LANE_Y, config.getLaneY()));
            config.setLaneWidth(data.getFloatExtra(HudAreaSelectActivity.EXTRA_LANE_WIDTH, config.getLaneWidth()));
            config.setLaneHeight(data.getFloatExtra(HudAreaSelectActivity.EXTRA_LANE_HEIGHT, config.getLaneHeight()));
            Toast.makeText(this, "区域配置已保存", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新区域状态显示
     */
    private void updateAreaStatus() {
        String hudInfo = String.format("HUD: %.0f%%×%.0f%% 位置(%.0f,%.0f)",
            config.getHudWidth(), config.getHudHeight(), config.getHudX(), config.getHudY());
        String laneInfo = String.format("车道: %.0f%%×%.0f%% 位置(%.0f,%.0f)",
            config.getLaneWidth(), config.getLaneHeight(), config.getLaneX(), config.getLaneY());
        Toast.makeText(this, hudInfo + "\n" + laneInfo, Toast.LENGTH_LONG).show();
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
