package com.amap.hudhelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * HUD 区域选择视图
 * 支持拖动选择区域、调整大小、显示网格
 */
public class HudAreaSelectorView extends View {
    
    // 区域类型
    public enum AreaType {
        HUD,      // HUD 导航区域
        LANE      // 车道信息区域
    }
    
    // 拖动类型 - 使用 int 标志位便于组合
    private static final int DRAG_NONE = 0;
    private static final int DRAG_MOVE = 1;
    private static final int DRAG_LEFT = 2;
    private static final int DRAG_RIGHT = 4;
    private static final int DRAG_TOP = 8;
    private static final int DRAG_BOTTOM = 16;
    
    // 画笔
    private Paint borderPaint;
    private Paint fillPaint;
    private Paint handlePaint;
    private Paint textPaint;
    private Paint gridPaint;
    
    // 区域
    private RectF hudRect = new RectF();
    private RectF laneRect = new RectF();
    private AreaType activeArea = AreaType.HUD;
    
    // 拖动状态
    private int dragType = DRAG_NONE;
    private float lastTouchX, lastTouchY;
    private float touchOffsetX, touchOffsetY;
    
    // 配置回调
    public interface OnAreaChangedListener {
        void onHudAreaChanged(float x, float y, float width, float height);
        void onLaneAreaChanged(float x, float y, float width, float height);
    }
    
    private OnAreaChangedListener listener;
    
    // 区域颜色
    private static final int HUD_COLOR = Color.parseColor("#40FF8800");  // 橙色半透明
    private static final int HUD_BORDER_COLOR = Color.parseColor("#FF8800");
    private static final int LANE_COLOR = Color.parseColor("#4000AAFF");  // 蓝色半透明
    private static final int LANE_BORDER_COLOR = Color.parseColor("#00AAFF");
    private static final int HANDLE_COLOR = Color.parseColor("#FFFFFF");
    
    public HudAreaSelectorView(Context context) {
        super(context);
        init();
    }
    
    public HudAreaSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public HudAreaSelectorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 边框画笔
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        
        // 填充画笔
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        
        // 把手画笔
        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(HANDLE_COLOR);
        
        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // 网格画笔
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(Color.parseColor("#30FFFFFF"));
    }
    
    public void setOnAreaChangedListener(OnAreaChangedListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置 HUD 区域（百分比 0-100）
     */
    public void setHudArea(float xPercent, float yPercent, float widthPercent, float heightPercent) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        
        hudRect.left = xPercent / 100f * w;
        hudRect.top = yPercent / 100f * h;
        hudRect.right = hudRect.left + widthPercent / 100f * w;
        hudRect.bottom = hudRect.top + heightPercent / 100f * h;
        
        invalidate();
    }
    
    /**
     * 设置车道区域（百分比 0-100）
     */
    public void setLaneArea(float xPercent, float yPercent, float widthPercent, float heightPercent) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        
        laneRect.left = xPercent / 100f * w;
        laneRect.top = yPercent / 100f * h;
        laneRect.right = laneRect.left + widthPercent / 100f * w;
        laneRect.bottom = laneRect.top + heightPercent / 100f * h;
        
        invalidate();
    }
    
    /**
     * 获取当前活动的区域
     */
    public AreaType getActiveArea() {
        return activeArea;
    }
    
    /**
     * 切换活动区域
     */
    public void setActiveArea(AreaType type) {
        activeArea = type;
        invalidate();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 初始区域设置（默认位置）
        if (oldw == 0 && oldh == 0) {
            hudRect.set(w * 0.4f, h * 0.65f, w * 0.6f, h * 0.85f);
            laneRect.set(w * 0.35f, h * 0.45f, w * 0.65f, h * 0.60f);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        
        // 绘制网格
        drawGrid(canvas, w, h);
        
        // 绘制 HUD 区域
        drawArea(canvas, hudRect, AreaType.HUD);
        
        // 绘制车道区域
        drawArea(canvas, laneRect, AreaType.LANE);
    }
    
    private void drawGrid(Canvas canvas, int w, int h) {
        // 绘制三分线
        canvas.drawLine(w / 3f, 0, w / 3f, h, gridPaint);
        canvas.drawLine(w * 2f / 3f, 0, w * 2f / 3f, h, gridPaint);
        canvas.drawLine(0, h / 2f, w, h / 2f, gridPaint);
        
        // 绘制中心点
        canvas.drawCircle(w / 2f, h / 2f, 10f, gridPaint);
    }
    
    private void drawArea(Canvas canvas, RectF rect, AreaType type) {
        boolean isActive = (type == activeArea);
        
        // 设置颜色
        if (type == AreaType.HUD) {
            fillPaint.setColor(HUD_COLOR);
            borderPaint.setColor(HUD_BORDER_COLOR);
        } else {
            fillPaint.setColor(LANE_COLOR);
            borderPaint.setColor(LANE_BORDER_COLOR);
        }
        
        // 加粗活动区域的边框
        borderPaint.setStrokeWidth(isActive ? 6f : 4f);
        
        // 绘制填充
        canvas.drawRect(rect, fillPaint);
        
        // 绘制边框
        canvas.drawRect(rect, borderPaint);
        
        // 绘制把手
        if (isActive) {
            drawHandles(canvas, rect);
        }
        
        // 绘制标签
        String label = type == AreaType.HUD ? "HUD导航" : "车道信息";
        canvas.drawText(label, rect.centerX(), rect.top - 15f, textPaint);
        
        // 绘制尺寸标注
        String size = String.format("%.0f%% x %.0f%%", 
            (rect.width() / getWidth()) * 100f,
            (rect.height() / getHeight()) * 100f);
        canvas.drawText(size, rect.centerX(), rect.bottom + 30f, textPaint);
    }
    
    private void drawHandles(Canvas canvas, RectF rect) {
        float handleRadius = 16f;
        
        // 四角把手
        canvas.drawCircle(rect.left, rect.top, handleRadius, handlePaint);
        canvas.drawCircle(rect.right, rect.top, handleRadius, handlePaint);
        canvas.drawCircle(rect.left, rect.bottom, handleRadius, handlePaint);
        canvas.drawCircle(rect.right, rect.bottom, handleRadius, handlePaint);
        
        // 四边中点把手
        canvas.drawCircle(rect.centerX(), rect.top, handleRadius * 0.7f, handlePaint);
        canvas.drawCircle(rect.centerX(), rect.bottom, handleRadius * 0.7f, handlePaint);
        canvas.drawCircle(rect.left, rect.centerY(), handleRadius * 0.7f, handlePaint);
        canvas.drawCircle(rect.right, rect.centerY(), handleRadius * 0.7f, handlePaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragType = getDragType(x, y);
                if (dragType != DragType.NONE) {
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (dragType != DragType.NONE) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    
                    RectF rect = activeArea == AreaType.HUD ? hudRect : laneRect;
                    resizeRect(rect, dragType, dx, dy);
                    
                    lastTouchX = x;
                    lastTouchY = y;
                    
                    notifyAreaChanged();
                    invalidate();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
                dragType = DRAG_NONE;
                break;
        }
        
        return super.onTouchEvent(event);
    }
    
    private int getDragType(float x, float y) {
        float tolerance = 40f;
        RectF hud = hudRect;
        RectF lane = laneRect;
        
        // 优先检查活动区域
        if (activeArea == AreaType.HUD) {
            if (isNear(x, y, hud.left, hud.top, tolerance)) return DRAG_LEFT | DRAG_TOP;
            if (isNear(x, y, hud.right, hud.top, tolerance)) return DRAG_RIGHT | DRAG_TOP;
            if (isNear(x, y, hud.left, hud.bottom, tolerance)) return DRAG_LEFT | DRAG_BOTTOM;
            if (isNear(x, y, hud.right, hud.bottom, tolerance)) return DRAG_RIGHT | DRAG_BOTTOM;
            if (isNear(x, y, hud.centerX(), hud.top, tolerance)) return DRAG_TOP;
            if (isNear(x, y, hud.centerX(), hud.bottom, tolerance)) return DRAG_BOTTOM;
            if (isNear(x, y, hud.left, hud.centerY(), tolerance)) return DRAG_LEFT;
            if (isNear(x, y, hud.right, hud.centerY(), tolerance)) return DRAG_RIGHT;
            if (hud.contains(x, y)) return DRAG_MOVE;
        }
        
        // 检查车道区域
        if (activeArea == AreaType.LANE) {
            if (isNear(x, y, lane.left, lane.top, tolerance)) return DRAG_LEFT | DRAG_TOP;
            if (isNear(x, y, lane.right, lane.top, tolerance)) return DRAG_RIGHT | DRAG_TOP;
            if (isNear(x, y, lane.left, lane.bottom, tolerance)) return DRAG_LEFT | DRAG_BOTTOM;
            if (isNear(x, y, lane.right, lane.bottom, tolerance)) return DRAG_RIGHT | DRAG_BOTTOM;
            if (isNear(x, y, lane.centerX(), lane.top, tolerance)) return DRAG_TOP;
            if (isNear(x, y, lane.centerX(), lane.bottom, tolerance)) return DRAG_BOTTOM;
            if (isNear(x, y, lane.left, lane.centerY(), tolerance)) return DRAG_LEFT;
            if (isNear(x, y, lane.right, lane.centerY(), tolerance)) return DRAG_RIGHT;
            if (lane.contains(x, y)) return DRAG_MOVE;
        }
        
        return DRAG_NONE;
    }
    
    private boolean isNear(float x, float y, float targetX, float targetY, float tolerance) {
        return Math.abs(x - targetX) <= tolerance && Math.abs(y - targetY) <= tolerance;
    }
    
    private void resizeRect(RectF rect, int type, float dx, float dy) {
        int w = getWidth();
        int h = getHeight();
        
        // 最小尺寸限制
        float minSize = Math.min(w, h) * 0.05f;
        
        // 边界限制
        float maxLeft = rect.right - minSize;
        float maxTop = rect.bottom - minSize;
        float maxRight = rect.left + minSize;
        float maxBottom = rect.top + minSize;
        
        if ((type & DRAG_LEFT) != 0) {
            float newLeft = Math.max(0, Math.min(rect.left + dx, maxLeft));
            if (rect.width() >= minSize) rect.left = newLeft;
        }
        if ((type & DRAG_RIGHT) != 0) {
            float newRight = Math.min(w, Math.max(rect.right + dx, maxRight));
            if (rect.width() >= minSize) rect.right = newRight;
        }
        if ((type & DRAG_TOP) != 0) {
            float newTop = Math.max(0, Math.min(rect.top + dy, maxTop));
            if (rect.height() >= minSize) rect.top = newTop;
        }
        if ((type & DRAG_BOTTOM) != 0) {
            float newBottom = Math.min(h, Math.max(rect.bottom + dy, maxBottom));
            if (rect.height() >= minSize) rect.bottom = newBottom;
        }
        if (type == DRAG_MOVE) {
            float newLeft = Math.max(0, Math.min(rect.left + dx, w - rect.width()));
            float newTop = Math.max(0, Math.min(rect.top + dy, h - rect.height()));
            rect.offsetTo(newLeft, newTop);
        }
    }
    
    private void notifyAreaChanged() {
        if (listener == null) return;
        
        int w = getWidth();
        int h = getHeight();
        
        if (activeArea == AreaType.HUD) {
            listener.onHudAreaChanged(
                (hudRect.left / w) * 100f,
                (hudRect.top / h) * 100f,
                (hudRect.width() / w) * 100f,
                (hudRect.height() / h) * 100f
            );
        } else {
            listener.onLaneAreaChanged(
                (laneRect.left / w) * 100f,
                (laneRect.top / h) * 100f,
                (laneRect.width() / w) * 100f,
                (laneRect.height() / h) * 100f
            );
        }
    }
}
