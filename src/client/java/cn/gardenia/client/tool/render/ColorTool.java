package cn.gardenia.client.tool.render;

import java.awt.Color;

public class ColorTool {
    public void init() {}

    public Color fromInt(int color) {
        return new Color(color);
    }

    public Color fromRGBA(int r, int g, int b, int a) {
        return new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255), clamp(a, 0, 255));
    }

    public Color fromHex(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() == 6) {
            return new Color(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            );
        } else if (hex.length() == 8) {
            return new Color(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16),
                    Integer.parseInt(hex.substring(6, 8), 16)
                );
        }
        return Color.WHITE;
    }

    public Color fromHSB(float hue, float saturation, float brightness) {
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public Color fromHSBA(float hue, float saturation, float brightness, int alpha) {
        Color c = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(alpha, 0, 255));
    }

    public Color rainbow(float speed, float offset) {
        float hue = (float)((System.currentTimeMillis() * speed / 1000.0 + offset) % 1.0);
        return new Color(Color.HSBtoRGB(hue, 0.8f, 1.0f));
    }

    public Color blend(Color a, Color b, float t) {
        t = clamp(t, 0.0f, 1.0f);
        return new Color(
                (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    public Color darker(Color c, float factor) {
        return new Color(
                Math.max(0, (int)(c.getRed() * factor)),
                Math.max(0, (int)(c.getGreen() * factor)),
                Math.max(0, (int)(c.getBlue() * factor)),
                c.getAlpha()
        );
    }

    public Color brighter(Color c, float factor) {
        return new Color(
                Math.min(255, (int)(c.getRed() / factor)),
                Math.min(255, (int)(c.getGreen() / factor)),
                Math.min(255, (int)(c.getBlue() / factor)),
                c.getAlpha()
        );
    }

    public Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(alpha, 0, 255));
    }

    public Color withAlpha(Color c, float alpha) {
        return withAlpha(c, (int)(alpha * 255.0f));
    }

    public int toInt(Color c) {
        return c.getRGB();
    }

    public String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public int argbToAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
