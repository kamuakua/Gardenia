package cn.gardenia.client.gui.nanovg;

import cn.gardenia.client.gui.theme.Theme;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import static cn.gardenia.client.gui.nanovg.NanoVGManager.INSTANCE;

public class NanoVGRender {

    public static void save() {
        NanoVG.nvgSave(INSTANCE.getContext());
    }

    public static void restore() {
        NanoVG.nvgRestore(INSTANCE.getContext());
    }

    public static void translate(float x, float y) {
        NanoVG.nvgTranslate(INSTANCE.getContext(), x, y);
    }

    public static void scale(float x, float y) {
        NanoVG.nvgScale(INSTANCE.getContext(), x, y);
    }

    public static void scissor(float x, float y, float w, float h) {
        NanoVG.nvgScissor(INSTANCE.getContext(), x, y, w, h);
    }

    public static void resetScissor() {
        NanoVG.nvgResetScissor(INSTANCE.getContext());
    }

    public static void globalAlpha(float alpha) {
        NanoVG.nvgGlobalAlpha(INSTANCE.getContext(), alpha);
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, float r, float g, float b, float a) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(r, g, b, a);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRoundedRectTop(float x, float y, float w, float h, float radius, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRectVarying(ctx, x, y, w, h, radius, radius, 0, 0);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRoundedRectBottom(float x, float y, float w, float h, float radius, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRectVarying(ctx, x, y, w, h, 0, 0, radius, radius);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRect(float x, float y, float w, float h, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x, y, w, h);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(r, g, b, a);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x, y, w, h);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawRoundedRectStroke(float x, float y, float w, float h, float radius, float strokeWidth, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgStrokeColor(ctx, nvgColor);
        NanoVG.nvgStrokeWidth(ctx, strokeWidth);
        NanoVG.nvgStroke(ctx);
        nvgColor.free();
    }

    public static void drawCircle(float cx, float cy, float r, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgCircle(ctx, cx, cy, r);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgFill(ctx);
        nvgColor.free();
    }

    public static void drawCircleStroke(float cx, float cy, float r, float strokeWidth, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgCircle(ctx, cx, cy, r);
        NanoVG.nvgStrokeColor(ctx, nvgColor);
        NanoVG.nvgStrokeWidth(ctx, strokeWidth);
        NanoVG.nvgStroke(ctx);
        nvgColor.free();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float width, int color) {
        long ctx = INSTANCE.getContext();
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgMoveTo(ctx, x1, y1);
        NanoVG.nvgLineTo(ctx, x2, y2);
        NanoVG.nvgStrokeColor(ctx, nvgColor);
        NanoVG.nvgStrokeWidth(ctx, width);
        NanoVG.nvgStroke(ctx);
        nvgColor.free();
    }

    public static void drawText(String text, float x, float y, int color, float size, String font) {
        long ctx = INSTANCE.getContext();
        int fontId = INSTANCE.getFont(font);
        if (fontId == -1) fontId = INSTANCE.getFont("regular");
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgFontFaceId(ctx, fontId);
        NanoVG.nvgFontSize(ctx, size);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgTextAlign(ctx, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(ctx, x, y, text);
        nvgColor.free();
    }

    public static void drawTextCenter(String text, float x, float y, int color, float size, String font) {
        long ctx = INSTANCE.getContext();
        int fontId = INSTANCE.getFont(font);
        if (fontId == -1) fontId = INSTANCE.getFont("regular");
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgFontFaceId(ctx, fontId);
        NanoVG.nvgFontSize(ctx, size);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgTextAlign(ctx, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(ctx, x, y, text);
        nvgColor.free();
    }

    public static void drawTextRight(String text, float x, float y, int color, float size, String font) {
        long ctx = INSTANCE.getContext();
        int fontId = INSTANCE.getFont(font);
        if (fontId == -1) fontId = INSTANCE.getFont("regular");
        NVGColor nvgColor = NanoVGManager.color(color);
        NanoVG.nvgFontFaceId(ctx, fontId);
        NanoVG.nvgFontSize(ctx, size);
        NanoVG.nvgFillColor(ctx, nvgColor);
        NanoVG.nvgTextAlign(ctx, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
        NanoVG.nvgText(ctx, x, y, text);
        nvgColor.free();
    }

    public static float textWidth(String text, float size, String font) {
        long ctx = INSTANCE.getContext();
        int fontId = INSTANCE.getFont(font);
        if (fontId == -1) fontId = INSTANCE.getFont("regular");
        NanoVG.nvgFontFaceId(ctx, fontId);
        NanoVG.nvgFontSize(ctx, size);
        float[] bounds = new float[4];
        return NanoVG.nvgTextBounds(ctx, 0, 0, text, bounds);
    }

    public static void drawGlassmorphism(float x, float y, float w, float h, float radius, float alpha) {
        long ctx = INSTANCE.getContext();

        int glassColor = Theme.GLASS_BG;
        int a = (int) ((alpha * 0.85f) * ((glassColor >> 24) & 0xFF) / 255f * 255) & 0xFF;
        int glass = (a << 24) | (glassColor & 0x00FFFFFF);
        NVGColor bg = NanoVGManager.color(glass);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgFillColor(ctx, bg);
        NanoVG.nvgFill(ctx);
        bg.free();

        int borderColor = Theme.GLASS_BORDER;
        int ba = (int) (alpha * ((borderColor >> 24) & 0xFF) / 255f * 255) & 0xFF;
        int border = (ba << 24) | (borderColor & 0x00FFFFFF);
        NVGColor borderNvg = NanoVGManager.color(border);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgStrokeColor(ctx, borderNvg);
        NanoVG.nvgStrokeWidth(ctx, 1.0f);
        NanoVG.nvgStroke(ctx);
        borderNvg.free();
    }

    public static void drawGlassmorphismLight(float x, float y, float w, float h, float radius, float alpha) {
        long ctx = INSTANCE.getContext();

        NVGColor bg = NanoVGManager.color(0.12f, 0.12f, 0.12f, alpha * 0.55f);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgFillColor(ctx, bg);
        NanoVG.nvgFill(ctx);
        bg.free();

        NVGColor border = NanoVGManager.color(1f, 1f, 1f, alpha * 0.06f);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgStrokeColor(ctx, border);
        NanoVG.nvgStrokeWidth(ctx, 1.0f);
        NanoVG.nvgStroke(ctx);
        border.free();
    }

    public static void drawShadow(float x, float y, float w, float h, float radius, float blur, float alpha) {
        long ctx = INSTANCE.getContext();
        org.lwjgl.nanovg.NVGPaint shadowPaint = org.lwjgl.nanovg.NVGPaint.calloc();
        NVGColor shadowColor = NanoVGManager.color(0.2f, 0.2f, 0.2f, alpha * 0.35f);
        NanoVG.nvgBoxGradient(ctx, x, y, w, h, radius, blur, shadowColor,
                NanoVGManager.color(0.2f, 0.2f, 0.2f, 0f), shadowPaint);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x - blur, y - blur, w + blur * 2, h + blur * 2);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        NanoVG.nvgPathWinding(ctx, NanoVG.NVG_HOLE);
        NanoVG.nvgFillPaint(ctx, shadowPaint);
        NanoVG.nvgFill(ctx);
        shadowColor.free();
        shadowPaint.free();
    }

    public static void drawImage(float x, float y, float w, float h, int imageId, float alpha) {
        long ctx = INSTANCE.getContext();
        org.lwjgl.nanovg.NVGPaint paint = org.lwjgl.nanovg.NVGPaint.calloc();
        NanoVG.nvgImagePattern(ctx, x, y, w, h, 0, imageId, alpha, paint);
        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRect(ctx, x, y, w, h);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        paint.free();
    }

    public static void drawGradientRect(float x, float y, float w, float h, float radius, int colorTop, int colorBottom) {
        long ctx = INSTANCE.getContext();
        NVGColor top = NanoVGManager.color(colorTop);
        NVGColor bottom = NanoVGManager.color(colorBottom);
        org.lwjgl.nanovg.NVGPaint paint = org.lwjgl.nanovg.NVGPaint.calloc();

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);

        NanoVG.nvgLinearGradient(ctx, x, y, x, y + h, top, bottom, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);

        top.free();
        bottom.free();
        paint.free();
    }
}
