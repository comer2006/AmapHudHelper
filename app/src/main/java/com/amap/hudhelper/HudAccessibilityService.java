package com.amap.hudhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 高德地图HUD辅助服务
 * 用于截取高德地图的HUD信息并显示到仪表盘
 */
public class HudAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "HudAccessibilityService";
    
    // 回调接口
    public interface HudDataCallback {
        void onHudDataUpdate(String direction, String distance, String speed, String road);
        void onLaneDataUpdate(String laneInfo);
    }
    
    private HudDataCallback callback;
    
    // 反射访问 dc0.p
    private static Field guideInfoField;
    private static Class<?> dc0Class;
    
    // 显示服务
    private Handler mainHandler;
    private HudPresentation presentation;
    private Display externalDisplay;
    private boolean isShowing = false;
    
    // 当前HUD数据
    private String currentDirection = "";
    private String currentDistance = "";
    private String currentSpeed = "";
    private String currentRoad = "";
    private String currentLane = "";
    
    // 静态初始化块 - 通过反射获取 dc0.p
    static {
        try {
            // 尝试多个可能的类名
            String[] classNames = {
                "com.autonavi.amapauto.dc0",
                "com.autonavi.amapauto.jni.dc0",
                "dc0"
            };
            
            for (String className : classNames) {
                try {
                    dc0Class = Class.forName(className);
                    guideInfoField = dc0Class.getField("p");
                    Log.d(TAG, "Found dc0 class: " + className);
                    break;
                } catch (ClassNotFoundException e) {
                    // 尝试下一个
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to init dc0 reflection", e);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "HudAccessibilityService created");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        // 配置 AccessibilityService
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        
        // 查找外部显示器
        detectExternalDisplay();
        
        Log.d(TAG, "Service connected, monitoring started");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 检查是否是高德地图
        if (!isAmapPackage(event)) {
            return;
        }
        
        // 尝试通过反射获取数据
        tryGetDataByReflection();
        
        // 通过界面分析获取数据
        parseHudFromUI(event);
    }
    
    /**
     * 检查是否是高德地图
     */
    private boolean isAmapPackage(AccessibilityEvent event) {
        if (event.getPackageName() == null) return false;
        String pkg = event.getPackageName().toString();
        return pkg.contains("autonavi") || 
               pkg.contains("amap") ||
               pkg.contains("高德");
    }
    
    /**
     * 通过反射获取 dc0.p 的数据
     */
    private void tryGetDataByReflection() {
        if (guideInfoField == null) return;
        
        try {
            Object guideInfo = guideInfoField.get(null);
            if (guideInfo == null) return;
            
            // 获取各项数据
            int iconType = ((Number) invokeMethod(guideInfo, "getIcon")).intValue();
            String direction = getIconText(iconType);
            int distance = ((Number) invokeMethod(guideInfo, "getSegRemainDis")).intValue();
            String distanceStr = formatDistance(distance);
            int speedVal = ((Number) invokeMethod(guideInfo, "getLimitedSpeed")).intValue();
            String speed = "限速" + speedVal;
            String road = (String) invokeMethod(guideInfo, "getCurRoadName");
            String nextRoad = (String) invokeMethod(guideInfo, "getNextRoadName");
            
            // 更新数据
            if (!isEmpty(direction)) currentDirection = direction;
            if (!isEmpty(distanceStr)) currentDistance = distanceStr;
            if (!isEmpty(speed)) currentSpeed = speed;
            if (!isEmpty(road)) currentRoad = road;
            
            // 发送更新
            sendHudUpdate();
            
        } catch (Exception e) {
            Log.d(TAG, "Reflection access failed: " + e.getMessage());
        }
    }
    
    /**
     * 通过界面分析获取HUD数据
     */
    private void parseHudFromUI(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = event.getSource();
        if (rootNode == null) return;
        
        try {
            // 递归搜索HUD相关文本
            searchHudTexts(rootNode, "");
        } finally {
            rootNode.recycle();
        }
    }
    
    /**
     * 递归搜索HUD文本
     */
    private void searchHudTexts(AccessibilityNodeInfo node, String path) {
        if (node == null) return;
        
        // 获取文本
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        
        if (!isEmpty(text)) {
            String txt = text.toString();
            
            // 识别距离（如 "300米"、"2公里"）
            if (txt.matches("\\d+米") || txt.matches("\\d+\\.\\d+公里")) {
                currentDistance = txt;
            }
            
            // 识别方向（如 "右转"、"左转"、"直行"）
            if (txt.contains("右转") || txt.contains("左转") || 
                txt.contains("直行") || txt.contains("环岛")) {
                currentDirection = txt;
            }
            
            // 识别限速（如 "60"）
            if (txt.matches("\\d+") && node.getParent() != null) {
                CharSequence parentText = node.getParent().getText();
                if (parentText != null && parentText.toString().contains("限")) {
                    currentSpeed = "限速" + txt;
                }
            }
        }
        
        // 递归搜索子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            searchHudTexts(child, path + " > " + node.getClassName());
            if (child != null) child.recycle();
        }
    }
    
    /**
     * 发送HUD更新到仪表盘
     */
    private void sendHudUpdate() {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onHudDataUpdate(
                    currentDirection,
                    currentDistance,
                    currentSpeed,
                    currentRoad
                );
            }
            
            // 更新仪表盘显示
            updatePresentation();
        });
    }
    
    /**
     * 检测外部显示器（仪表盘）
     */
    private void detectExternalDisplay() {
        mainHandler.postDelayed(() -> {
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            Display[] displays = dm.getDisplays();
            
            for (Display display : displays) {
                String name = display.getName().toLowerCase();
                if (display.getDisplayId() != android.view.Display.DEFAULT_DISPLAY &&
                    (name.contains("mirror") || name.contains("hdmi") || name.contains("presentation"))) {
                    externalDisplay = display;
                    Log.d(TAG, "Found external display: " + display.getName());
                    showPresentation();
                    break;
                }
            }
        }, 2000);
    }
    
    /**
     * 显示仪表盘
     */
    private void showPresentation() {
        if (externalDisplay == null || isShowing) return;
        
        try {
            presentation = new HudPresentation(this, externalDisplay);
            presentation.show();
            isShowing = true;
            Log.d(TAG, "Presentation shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show presentation", e);
        }
    }
    
    /**
     * 更新仪表盘显示
     */
    private void updatePresentation() {
        if (presentation != null && isShowing) {
            presentation.updateHud(
                currentDirection.isEmpty() ? "导航中" : currentDirection,
                currentDistance.isEmpty() ? "--" : currentDistance,
                currentSpeed.isEmpty() ? "" : currentSpeed,
                currentRoad.isEmpty() ? "" : currentRoad
            );
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
        isShowing = false;
        Log.d(TAG, "Service destroyed");
    }
    
    // ====== 工具方法 ======
    
    private Object invokeMethod(Object obj, String methodName) throws Exception {
        Object result = obj.getClass().getMethod(methodName).invoke(obj);
        return result != null ? result : Integer.valueOf(0);
    }
    
    private int invokeIntMethod(Object obj, String methodName) throws Exception {
        Object result = invokeMethod(obj, methodName);
        return result instanceof Integer ? (Integer) result : 0;
    }
    
    private boolean isEmpty(CharSequence text) {
        return text == null || text.toString().trim().isEmpty();
    }
    
    private String formatDistance(int meters) {
        if (meters < 1000) {
            return meters + "米";
        } else {
            return String.format("%.1f公里", meters / 1000.0);
        }
    }
    
    private String getIconText(int iconType) {
        switch (iconType) {
            case 0x17: return "左转";
            case 0x19:
            case 0x1a:
            case 0x1c: return "右转";
            case 0x15:
            case 0x16:
            case 0x18: return "直行";
            case 0x1b: return "环岛";
            default: return "";
        }
    }
    
    // ====== 仪表盘显示类 ======
    
    public class HudPresentation extends Presentation {
        
        private TextView dirText, distText, speedText, roadText, laneText;
        private LinearLayout hudLayout, laneLayout;
        private Handler handler;
        private HudDisplayConfig displayConfig;
        
        public HudPresentation(Context context, Display display) {
            super(context, display);
            displayConfig = HudDisplayConfig.getInstance(context);
        }
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            DisplayMetrics metrics = new DisplayMetrics();
            getDisplay().getRealMetrics(metrics);
            
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            
            Log.d(TAG, "Presentation display: " + screenWidth + "x" + screenHeight);
            
            // 创建根布局
            FrameLayout root = new FrameLayout(getContext());
            root.setBackgroundColor(Color.BLACK);
            
            // 根据配置计算 HUD 区域位置和大小（百分比转像素）
            float hudX = displayConfig.getHudX() / 100f;
            float hudY = displayConfig.getHudY() / 100f;
            float hudW = displayConfig.getHudWidth() / 100f;
            float hudH = displayConfig.getHudHeight() / 100f;
            
            int hudWidth = (int) (screenWidth * hudW);
            int hudHeight = (int) (screenHeight * hudH);
            int hudLeft = (int) (screenWidth * hudX);
            int hudTop = (int) (screenHeight * hudY);
            
            // 创建 HUD 布局
            hudLayout = new LinearLayout(getContext());
            hudLayout.setOrientation(LinearLayout.VERTICAL);
            hudLayout.setGravity(Gravity.CENTER);
            hudLayout.setBackgroundColor(Color.parseColor("#CC404040"));
            hudLayout.setPadding(15, 10, 15, 10);
            
            FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(hudWidth, hudHeight);
            hudParams.leftMargin = hudLeft;
            hudParams.topMargin = hudTop;
            
            // 方向
            dirText = new TextView(getContext());
            dirText.setTextSize(28);
            dirText.setTextColor(Color.WHITE);
            dirText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            dirText.setGravity(Gravity.CENTER);
            dirText.setText("导航中");
            
            // 距离
            distText = new TextView(getContext());
            distText.setTextSize(24);
            distText.setTextColor(Color.parseColor("#00FF00"));
            distText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            distText.setGravity(Gravity.CENTER);
            distText.setText("--");
            
            // 限速
            speedText = new TextView(getContext());
            speedText.setTextSize(16);
            speedText.setTextColor(Color.parseColor("#FFA500"));
            speedText.setGravity(Gravity.CENTER);
            
            // 路名
            roadText = new TextView(getContext());
            roadText.setTextSize(14);
            roadText.setTextColor(Color.LTGRAY);
            roadText.setGravity(Gravity.CENTER);
            
            hudLayout.addView(dirText);
            hudLayout.addView(distText);
            hudLayout.addView(speedText);
            hudLayout.addView(roadText);
            
            // 车道区域
            float laneX = displayConfig.getLaneX() / 100f;
            float laneY = displayConfig.getLaneY() / 100f;
            float laneW = displayConfig.getLaneWidth() / 100f;
            float laneH = displayConfig.getLaneHeight() / 100f;
            
            int laneWidth = (int) (screenWidth * laneW);
            int laneHeight = (int) (screenHeight * laneH);
            int laneLeft = (int) (screenWidth * laneX);
            int laneTop = (int) (screenHeight * laneY);
            
            laneLayout = new LinearLayout(getContext());
            laneLayout.setOrientation(LinearLayout.VERTICAL);
            laneLayout.setGravity(Gravity.CENTER);
            laneLayout.setBackgroundColor(Color.parseColor("#CC003366"));
            laneLayout.setPadding(10, 8, 10, 8);
            
            FrameLayout.LayoutParams laneParams = new FrameLayout.LayoutParams(laneWidth, laneHeight);
            laneParams.leftMargin = laneLeft;
            laneParams.topMargin = laneTop;
            
            laneText = new TextView(getContext());
            laneText.setTextSize(18);
            laneText.setTextColor(Color.parseColor("#00AAFF"));
            laneText.setGravity(Gravity.CENTER);
            laneText.setText("车道信息");
            
            laneLayout.addView(laneText);
            
            root.addView(hudLayout, hudParams);
            root.addView(laneLayout, laneParams);
            
            setContentView(root);
            
            handler = new Handler(Looper.getMainLooper());
        }
        
        public void updateHud(String direction, String distance, String speed, String road) {
            if (handler == null) return;
            handler.post(() -> {
                try {
                    if (dirText != null) dirText.setText(direction);
                    if (distText != null) distText.setText(distance);
                    if (speedText != null) speedText.setText(speed);
                    if (roadText != null) roadText.setText(road);
                } catch (Exception e) {
                    Log.e(TAG, "Update error", e);
                }
            });
        }
        
        public void updateLane(String laneInfo) {
            if (handler == null) return;
            handler.post(() -> {
                try {
                    if (laneText != null) laneText.setText(laneInfo);
                } catch (Exception e) {
                    Log.e(TAG, "Update lane error", e);
                }
            });
        }
        
        @Override
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            isShowing = false;
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        }
    }
}
