package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import org.lwjgl.nanovg.NanoVG;

import java.util.function.Consumer;

import static cn.gardenia.client.gui.nanovg.NanoVGManager.INSTANCE;

public class ColorPicker extends Component {
    private String label;
    private int color;
    private Consumer<Integer> onChange;
    private float textSize = 12;
    private String font = "regular";
    private boolean expanded = false;
    private Animation expandAnimation;
    private float pickerWidth = 200;
    private float pickerHeight = 280;
    private float hueBarHeight = 14;
    private float padding = 10;
    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private float[] hsv = new float[3];

    private static final int[][] PRESET_COLORS = {
        {0xFFFFFFFF, 0xFF000000, 0xFF808080, 0xFFC0C0C0, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00},
        {0xFFFF00FF, 0xFF00FFFF, 0xFFFFA500, 0xFF800080, 0xFF008000, 0xFF000080, 0xFF800000, 0xFF008080},
        {0xFFFFC0CB, 0xFFFFE4B5, 0xFFE6E6FA, 0xFFF0E68C, 0xFF98FB98, 0xFF87CEEB, 0xFFF4A460, 0xFFDDA0DD},
        {0xFFFF6B6B, 0xFF4ECDC4, 0xFF45B7D1, 0xFF96CEB4, 0xFFECC8A8, 0xFFFFEAA7, 0xFFDDA0DD, 0xFF74B9FF}
    };
    private float presetCellSize = 18;
    private float presetGap = 4;
    private float presetMarginTop = 8;
    private float collapsedHeight = 32;
    private float expandedExtraHeight = 0;

    public ColorPicker(float x, float y, float width, float height, String label, int initialColor) {
        super(x, y, width, height);
        this.label = label;
        this.color = initialColor;
        this.hsv = rgbToHsv(initialColor);
        this.expandAnimation = new Animation(0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_QUAD);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();
        expandAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float expand = expandAnimation.getCurrent();
        
        // Update height based on expansion
        float targetExtraHeight = expanded ? pickerHeight + 8 : 0;
        expandedExtraHeight += (targetExtraHeight - expandedExtraHeight) * 0.3f;
        this.height = collapsedHeight + expandedExtraHeight;

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        NanoVGRender.drawText(label, x + 8, y + 10,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), textSize, font);

        float previewX = x + width - 36;
        float previewY = y + 4;
        float previewSize = 20;

        NanoVGRender.drawRoundedRect(previewX - 1, previewY - 1, previewSize + 2, previewSize + 2, 6,
                Theme.withAlpha(Theme.BORDER, alpha));
        NanoVGRender.drawRoundedRect(previewX, previewY, previewSize, previewSize, 5,
                Theme.withAlpha(color, alpha));

        if (expand > 0.01f) {
            float pickerX = x + width - pickerWidth - 8;
            float pickerY = y + 32;
            float totalHeight = pickerHeight * expand;

            NanoVGRender.scissor(pickerX, pickerY, pickerWidth, totalHeight);
            NanoVGRender.drawGlassmorphism(pickerX, pickerY, pickerWidth, pickerHeight, Theme.CORNER_RADIUS_LARGE, alpha);

            float svX = pickerX + padding;
            float svY = pickerY + padding;
            float svSize = pickerWidth - padding * 2;
            float svHeight = 120;

            drawSVGradient(svX, svY, svSize, svHeight);

            float sat = hsv[1];
            float val = hsv[2];
            float cursorX = svX + sat * svSize;
            float cursorY = svY + (1 - val) * svHeight;
            NanoVGRender.drawCircleStroke(cursorX, cursorY, 5, 2, Theme.withAlpha(0xFFFFFFFF, alpha));
            NanoVGRender.drawCircle(cursorX, cursorY, 3, Theme.withAlpha(color, alpha));

            float hueY = svY + svHeight + padding;
            float hueWidth = svSize;
            drawHueBar(svX, hueY, hueWidth, hueBarHeight);

            float hueCursorX = svX + hsv[0] * hueWidth;
            NanoVGRender.drawCircleStroke(hueCursorX, hueY + hueBarHeight / 2, 6, 2, Theme.withAlpha(0xFFFFFFFF, alpha));
            NanoVGRender.drawCircle(hueCursorX, hueY + hueBarHeight / 2, 4, Theme.withAlpha(hsvToRgb(hsv[0], 1, 1), alpha));

            float presetY = hueY + hueBarHeight + presetMarginTop;
            drawPresetPalette(pickerX + padding, presetY, svSize, alpha);

            NanoVGRender.resetScissor();
        }

        NanoVGRender.restore();
    }

    private void drawSVGradient(float x, float y, float w, float h) {
        long ctx = INSTANCE.getContext();
        org.lwjgl.nanovg.NVGPaint paint1 = org.lwjgl.nanovg.NVGPaint.calloc();
        org.lwjgl.nanovg.NVGPaint paint2 = org.lwjgl.nanovg.NVGPaint.calloc();

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x, y, w, h);
        NanoVG.nvgLinearGradient(ctx, x, y, x + w, y,
                NanoVGManager.color(0xFFFFFFFF), NanoVGManager.color(hsvToRgb(hsv[0], 1, 1)), paint1);
        NanoVG.nvgFillPaint(ctx, paint1);
        NanoVG.nvgFill(ctx);

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x, y, w, h);
        NanoVG.nvgLinearGradient(ctx, x, y, x, y + h,
                NanoVGManager.color(0x00FFFFFF), NanoVGManager.color(0xFF000000), paint2);
        NanoVG.nvgFillPaint(ctx, paint2);
        NanoVG.nvgFill(ctx);

        paint1.free();
        paint2.free();
    }

    private void drawHueBar(float x, float y, float w, float h) {
        long ctx = INSTANCE.getContext();
        float step = w / 6;

        for (int i = 0; i < 6; i++) {
            float sx = x + i * step;
            float sw = (i == 5) ? w - i * step : step;
            int c1 = hsvToRgb(i / 6f, 1, 1);
            int c2 = hsvToRgb((i + 1) / 6f, 1, 1);

            NanoVG.nvgBeginPath(ctx);
            if (i == 0) {
                NanoVG.nvgRoundedRectVarying(ctx, sx, y, sw, h, 4, 4, 0, 0);
            } else if (i == 5) {
                NanoVG.nvgRoundedRectVarying(ctx, sx, y, sw, h, 0, 0, 4, 4);
            } else {
                NanoVG.nvgRect(ctx, sx, y, sw, h);
            }
            org.lwjgl.nanovg.NVGPaint paint = org.lwjgl.nanovg.NVGPaint.calloc();
            NanoVG.nvgLinearGradient(ctx, sx, y, sx + sw, y,
                    NanoVGManager.color(c1), NanoVGManager.color(c2), paint);
            NanoVG.nvgFillPaint(ctx, paint);
            NanoVG.nvgFill(ctx);
            paint.free();
        }
    }

    private void drawPresetPalette(float x, float y, float w, float alpha) {
        int cols = PRESET_COLORS[0].length;
        int rows = PRESET_COLORS.length;
        float totalPaletteWidth = cols * presetCellSize + (cols - 1) * presetGap;
        float startX = x + (w - totalPaletteWidth) / 2f;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cx = startX + col * (presetCellSize + presetGap);
                float cy = y + row * (presetCellSize + presetGap);
                int presetColor = PRESET_COLORS[row][col];

                NanoVGRender.drawRoundedRect(cx, cy, presetCellSize, presetCellSize, 4,
                        Theme.withAlpha(presetColor, alpha));
                NanoVGRender.drawRoundedRectStroke(cx, cy, presetCellSize, presetCellSize, 4, 1,
                        Theme.withAlpha(0x20000000, alpha));
            }
        }
    }

    @Override
    protected void onClick(double mouseX, double mouseY, int button) {
        float previewX = x + width - 36;
        float previewY = y + 4;
        if (isMouseOver(mouseX, mouseY, previewX, previewY, 20, 20)) {
            expanded = !expanded;
            expandAnimation.setTarget(expanded ? 1 : 0);
            return;
        }

        if (expanded) {
            float pickerX = x + width - pickerWidth - 8;
            float pickerY = y + 32;
            float svX = pickerX + padding;
            float svY = pickerY + padding;
            float svSize = pickerWidth - padding * 2;
            float svHeight = 120;

            if (isMouseOver(mouseX, mouseY, svX, svY, svSize, svHeight)) {
                draggingSV = true;
                updateSV(mouseX, mouseY, svX, svY, svSize, svHeight);
                return;
            }

            float hueY = svY + svHeight + padding;
            if (isMouseOver(mouseX, mouseY, svX, hueY, svSize, hueBarHeight)) {
                draggingHue = true;
                updateHue(mouseX, svX, svSize);
                return;
            }

            if (handlePresetClick(mouseX, mouseY, pickerX + padding, hueY + hueBarHeight + presetMarginTop, svSize)) {
                return;
            }

            expanded = false;
            expandAnimation.setTarget(0);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (expanded) {
            float pickerX = x + width - pickerWidth - 8;
            float pickerY = y + 32;
            float svX = pickerX + padding;
            float svY = pickerY + padding;
            float svSize = pickerWidth - padding * 2;
            float svHeight = 120;

            if (draggingSV) {
                updateSV(mouseX, mouseY, svX, svY, svSize, svHeight);
            }
            if (draggingHue) {
                updateHue(mouseX, svX, svSize);
            }
        }
    }

    @Override
    protected void onRelease(double mouseX, double mouseY, int button) {
        draggingSV = false;
        draggingHue = false;
    }

    private void updateSV(double mouseX, double mouseY, float svX, float svY, float svW, float svH) {
        float s = (float) Math.max(0, Math.min(1, (mouseX - svX) / svW));
        float v = (float) Math.max(0, Math.min(1, 1 - (mouseY - svY) / svH));
        hsv[1] = s;
        hsv[2] = v;
        updateColor();
    }

    private void updateHue(double mouseX, float hueX, float hueW) {
        float h = (float) Math.max(0, Math.min(1, (mouseX - hueX) / hueW));
        hsv[0] = h;
        updateColor();
    }

    private void updateColor() {
        color = hsvToRgb(hsv[0], hsv[1], hsv[2]);
        if (onChange != null) {
            onChange.accept(color);
        }
    }

    private boolean handlePresetClick(double mouseX, double mouseY, float paletteX, float paletteY, float paletteW) {
        int cols = PRESET_COLORS[0].length;
        int rows = PRESET_COLORS.length;
        float totalPaletteWidth = cols * presetCellSize + (cols - 1) * presetGap;
        float startX = paletteX + (paletteW - totalPaletteWidth) / 2f;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cx = startX + col * (presetCellSize + presetGap);
                float cy = paletteY + row * (presetCellSize + presetGap);
                if (isMouseOver(mouseX, mouseY, cx, cy, presetCellSize, presetCellSize)) {
                    int presetColor = PRESET_COLORS[row][col];
                    color = presetColor;
                    hsv = rgbToHsv(presetColor);
                    if (onChange != null) {
                        onChange.accept(color);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void setOnChange(Consumer<Integer> onChange) {
        this.onChange = onChange;
    }

    public int getColor() { return color; }
    public void setColor(int color) {
        this.color = color;
        this.hsv = rgbToHsv(color);
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d + 6) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
        }
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    private static int hsvToRgb(float h, float s, float v) {
        float r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
