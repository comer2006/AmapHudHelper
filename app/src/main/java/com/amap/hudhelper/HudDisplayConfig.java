package com.amap.hudhelper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * HUD 显示区域配置管理器
 * 保存用户在仪表盘上选择的 HUD 区域位置和大小
 */
public class HudDisplayConfig {
    
    private static final String PREFS_NAME = "hud_display_config";
    private static final String KEY_HUD_X = "hud_x";
    private static final String KEY_HUD_Y = "hud_y";
    private static final String KEY_HUD_WIDTH = "hud_width";
    private static final String KEY_HUD_HEIGHT = "hud_height";
    private static final String KEY_LANE_X = "lane_x";
    private static final String KEY_LANE_Y = "lane_y";
    private static final String KEY_LANE_WIDTH = "lane_width";
    private static final String KEY_LANE_HEIGHT = "lane_height";
    private static final String KEY_EDIT_MODE = "edit_mode";
    private static final String KEY_FIRST_RUN = "first_run";
    
    // 默认值（百分比，0-100）
    private static final float DEFAULT_HUD_X = 40f;       // 居中偏右
    private static final float DEFAULT_HUD_Y = 75f;       // 底部 1/4
    private static final float DEFAULT_HUD_WIDTH = 20f;   // 屏幕的 1/5
    private static final float DEFAULT_HUD_HEIGHT = 20f;   // 屏幕的 1/5
    
    private static final float DEFAULT_LANE_X = 40f;      // 居中
    private static final float DEFAULT_LANE_Y = 50f;     // 中间偏下
    private static final float DEFAULT_LANE_WIDTH = 20f;
    private static final float DEFAULT_LANE_HEIGHT = 15f;
    
    private final SharedPreferences prefs;
    private static HudDisplayConfig instance;
    
    private HudDisplayConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized HudDisplayConfig getInstance(Context context) {
        if (instance == null) {
            instance = new HudDisplayConfig(context);
        }
        return instance;
    }
    
    // HUD 区域配置
    public float getHudX() { return prefs.getFloat(KEY_HUD_X, DEFAULT_HUD_X); }
    public float getHudY() { return prefs.getFloat(KEY_HUD_Y, DEFAULT_HUD_Y); }
    public float getHudWidth() { return prefs.getFloat(KEY_HUD_WIDTH, DEFAULT_HUD_WIDTH); }
    public float getHudHeight() { return prefs.getFloat(KEY_HUD_HEIGHT, DEFAULT_HUD_HEIGHT); }
    
    public void setHudX(float x) { prefs.edit().putFloat(KEY_HUD_X, x).apply(); }
    public void setHudY(float y) { prefs.edit().putFloat(KEY_HUD_Y, y).apply(); }
    public void setHudWidth(float w) { prefs.edit().putFloat(KEY_HUD_WIDTH, w).apply(); }
    public void setHudHeight(float h) { prefs.edit().putFloat(KEY_HUD_HEIGHT, h).apply(); }
    
    // 车道区域配置
    public float getLaneX() { return prefs.getFloat(KEY_LANE_X, DEFAULT_LANE_X); }
    public float getLaneY() { return prefs.getFloat(KEY_LANE_Y, DEFAULT_LANE_Y); }
    public float getLaneWidth() { return prefs.getFloat(KEY_LANE_WIDTH, DEFAULT_LANE_WIDTH); }
    public float getLaneHeight() { return prefs.getFloat(KEY_LANE_HEIGHT, DEFAULT_LANE_HEIGHT); }
    
    public void setLaneX(float x) { prefs.edit().putFloat(KEY_LANE_X, x).apply(); }
    public void setLaneY(float y) { prefs.edit().putFloat(KEY_LANE_Y, y).apply(); }
    public void setLaneWidth(float w) { prefs.edit().putFloat(KEY_LANE_WIDTH, w).apply(); }
    public void setLaneHeight(float h) { prefs.edit().putFloat(KEY_LANE_HEIGHT, h).apply(); }
    
    // 编辑模式
    public boolean isEditMode() { return prefs.getBoolean(KEY_EDIT_MODE, false); }
    public void setEditMode(boolean enabled) { prefs.edit().putBoolean(KEY_EDIT_MODE, enabled).apply(); }
    
    // 首次运行检测
    public boolean isFirstRun() {
        boolean first = prefs.getBoolean(KEY_FIRST_RUN, true);
        if (first) {
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        }
        return first;
    }
    
    // 重置为默认
    public void resetToDefault() {
        prefs.edit()
            .putFloat(KEY_HUD_X, DEFAULT_HUD_X)
            .putFloat(KEY_HUD_Y, DEFAULT_HUD_Y)
            .putFloat(KEY_HUD_WIDTH, DEFAULT_HUD_WIDTH)
            .putFloat(KEY_HUD_HEIGHT, DEFAULT_HUD_HEIGHT)
            .putFloat(KEY_LANE_X, DEFAULT_LANE_X)
            .putFloat(KEY_LANE_Y, DEFAULT_LANE_Y)
            .putFloat(KEY_LANE_WIDTH, DEFAULT_LANE_WIDTH)
            .putFloat(KEY_LANE_HEIGHT, DEFAULT_LANE_HEIGHT)
            .apply();
    }
}
