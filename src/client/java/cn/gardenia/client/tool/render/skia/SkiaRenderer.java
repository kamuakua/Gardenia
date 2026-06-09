package cn.gardenia.client.tool.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.awt.Color;

public final class SkiaRenderer {
    private SkiaRenderer() {
    }

    public static Canvas canvas() {
        return SkiaContext.getCanvas();
    }

    public static void save() {
        Canvas canvas = canvas();
        if (canvas != null) {
            canvas.save();
        }
    }

    public static void restore() {
        Canvas canvas = canvas();
        if (canvas != null) {
            canvas.restore();
        }
    }

    public static void scale(float factor) {
        Canvas canvas = canvas();
        if (canvas != null) {
            canvas.scale(factor, factor);
        }
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        Canvas canvas = canvas();
        if (canvas == null) {
            return;
        }
        try (Paint paint = paint(color)) {
            canvas.drawRect(Rect.makeXYWH(x, y, width, height), paint);
        }
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        Canvas canvas = canvas();
        if (canvas == null) {
            return;
        }
        try (Paint paint = paint(color)) {
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
        }
    }

    public static void drawOutline(float x, float y, float width, float height, float radius, float strokeWidth, Color color) {
        Canvas canvas = canvas();
        if (canvas == null) {
            return;
        }
        try (Paint paint = paint(color)) {
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(strokeWidth);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
        }
    }

    public static void drawText(String text, float x, float y, Color color, Font font) {
        Canvas canvas = canvas();
        if (canvas == null || text == null || font == null) {
            return;
        }
        try (Paint paint = paint(color)) {
            canvas.drawString(text, x, y, font, paint);
        }
    }

    public static float getStringWidth(String text, Font font) {
        if (text == null || font == null) {
            return 0.0F;
        }
        return font.measureTextWidth(text);
    }

    private static Paint paint(Color color) {
        Paint paint = new Paint();
        paint.setColor(color == null ? 0xFFFFFFFF : color.getRGB());
        return paint;
    }
}
