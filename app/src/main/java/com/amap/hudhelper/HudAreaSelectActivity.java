package com.amap.hudhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * HUD 显示区域选择界面
 * 在主屏幕上可视化选择 HUD 和车道信息的显示区域
 */
public class HudAreaSelectActivity extends Activity {
    
    public static final String EXTRA_HUD_X = "hud_x";
    public static final String EXTRA_HUD_Y = "hud_y";
    public static final String EXTRA_HUD_WIDTH = "hud_width";
    public static final String EXTRA_HUD_HEIGHT = "hud_height";
    public static final String EXTRA_LANE_X = "lane_x";
    public static final String EXTRA_LANE_Y = "lane_y";
    public static final String EXTRA_LANE_WIDTH = "lane_width";
    public static final String EXTRA_LANE_HEIGHT = "lane_height";
    
    private HudDisplayConfig config;
    private HudAreaSelectorView selectorView;
    
    // 微调控件
    private SeekBar widthSeekBar, heightSeekBar;
    private TextView widthValueText, heightValueText;
    private RadioGroup areaRadioGroup;
    private Button resetBtn, saveBtn, previewBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 全屏显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        config = HudDisplayConfig.getInstance(this);
        
        setContentView(R.layout.activity_area_select);
        
        initViews();
        loadCurrentConfig();
        setupListeners();
    }
    
    private void initViews() {
        selectorView = findViewById(R.id.areaSelectorView);
        widthSeekBar = findViewById(R.id.widthSeekBar);
        heightSeekBar = findViewById(R.id.heightSeekBar);
        widthValueText = findViewById(R.id.widthValueText);
        heightValueText = findViewById(R.id.heightValueText);
        areaRadioGroup = findViewById(R.id.areaRadioGroup);
        resetBtn = findViewById(R.id.resetButton);
        saveBtn = findViewById(R.id.saveButton);
        previewBtn = findViewById(R.id.previewButton);
    }
    
    private void loadCurrentConfig() {
        // 从配置加载区域
        float hudX = config.getHudX();
        float hudY = config.getHudY();
        float hudW = config.getHudWidth();
        float hudH = config.getHudHeight();
        
        float laneX = config.getLaneX();
        float laneY = config.getLaneY();
        float laneW = config.getLaneWidth();
        float laneH = config.getLaneHeight();
        
        selectorView.setHudArea(hudX, hudY, hudW, hudH);
        selectorView.setLaneArea(laneX, laneY, laneW, laneH);
        
        // 设置初始微调值
        HudAreaSelectorView.AreaType active = selectorView.getActiveArea();
        if (active == HudAreaSelectorView.AreaType.HUD) {
            widthSeekBar.setProgress((int) hudW);
            heightSeekBar.setProgress((int) hudH);
        } else {
            widthSeekBar.setProgress((int) laneW);
            heightSeekBar.setProgress((int) laneH);
        }
        updateWidthHeightText();
    }
    
    private void setupListeners() {
        // 区域切换
        areaRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioHud) {
                selectorView.setActiveArea(HudAreaSelectorView.AreaType.HUD);
            } else {
                selectorView.setActiveArea(HudAreaSelectorView.AreaType.LANE);
            }
            updateWidthHeightFromSelector();
        });
        
        // 区域变化回调
        selectorView.setOnAreaChangedListener(new HudAreaSelectorView.OnAreaChangedListener() {
            @Override
            public void onHudAreaChanged(float x, float y, float width, float height) {
                config.setHudX(x);
                config.setHudY(y);
                config.setHudWidth(width);
                config.setHudHeight(height);
                updateWidthHeightText();
            }
            
            @Override
            public void onLaneAreaChanged(float x, float y, float width, float height) {
                config.setLaneX(x);
                config.setLaneY(y);
                config.setLaneWidth(width);
                config.setLaneHeight(height);
                updateWidthHeightText();
            }
        });
        
        // 宽度微调
        widthSeekBar.setMax(50); // 最大 50%
        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newWidth = progress + 5; // 最小 5%
                    HudAreaSelectorView.AreaType active = selectorView.getActiveArea();
                    if (active == HudAreaSelectorView.AreaType.HUD) {
                        config.setHudWidth(newWidth);
                        selectorView.setHudArea(config.getHudX(), config.getHudY(), newWidth, config.getHudHeight());
                    } else {
                        config.setLaneWidth(newWidth);
                        selectorView.setLaneArea(config.getLaneX(), config.getLaneY(), newWidth, config.getLaneHeight());
                    }
                    updateWidthHeightText();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 高度微调
        heightSeekBar.setMax(50);
        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newHeight = progress + 5;
                    HudAreaSelectorView.AreaType active = selectorView.getActiveArea();
                    if (active == HudAreaSelectorView.AreaType.HUD) {
                        config.setHudHeight(newHeight);
                        selectorView.setHudArea(config.getHudX(), config.getHudY(), config.getHudWidth(), newHeight);
                    } else {
                        config.setLaneHeight(newHeight);
                        selectorView.setLaneArea(config.getLaneX(), config.getLaneY(), config.getLaneWidth(), newHeight);
                    }
                    updateWidthHeightText();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 重置按钮
        resetBtn.setOnClickListener(v -> {
            config.resetToDefault();
            loadCurrentConfig();
        });
        
        // 保存并启动服务
        saveBtn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_HUD_X, config.getHudX());
            resultIntent.putExtra(EXTRA_HUD_Y, config.getHudY());
            resultIntent.putExtra(EXTRA_HUD_WIDTH, config.getHudWidth());
            resultIntent.putExtra(EXTRA_HUD_HEIGHT, config.getHudHeight());
            resultIntent.putExtra(EXTRA_LANE_X, config.getLaneX());
            resultIntent.putExtra(EXTRA_LANE_Y, config.getLaneY());
            resultIntent.putExtra(EXTRA_LANE_WIDTH, config.getLaneWidth());
            resultIntent.putExtra(EXTRA_LANE_HEIGHT, config.getLaneHeight());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        
        // 预览按钮（临时进入编辑模式）
        previewBtn.setOnClickListener(v -> {
            config.setEditMode(true);
            // 启动 AccessibilityService
            Intent serviceIntent = new Intent(this, HudAccessibilityService.class);
            startService(serviceIntent);
        });
    }
    
    private void updateWidthHeightFromSelector() {
        HudAreaSelectorView.AreaType active = selectorView.getActiveArea();
        float w, h;
        if (active == HudAreaSelectorView.AreaType.HUD) {
            w = config.getHudWidth();
            h = config.getHudHeight();
        } else {
            w = config.getLaneWidth();
            h = config.getLaneHeight();
        }
        widthSeekBar.setProgress((int) w - 5);
        heightSeekBar.setProgress((int) h - 5);
        updateWidthHeightText();
    }
    
    private void updateWidthHeightText() {
        widthValueText.setText(String.format("%.0f%%", widthSeekBar.getProgress() + 5));
        heightValueText.setText(String.format("%.0f%%", heightSeekBar.getProgress() + 5));
    }
}
