package cn.gardenia.client.gui.theme;

public class Theme {
    public static final int BACKGROUND = 0xFFF5F5F5;
    public static final int BACKGROUND_SECONDARY = 0xFFEEEEEE;
    public static final int BACKGROUND_TERTIARY = 0xFFE0E0E0;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int SURFACE_HOVER = 0xFFF0F0F0;
    public static final int SURFACE_ACTIVE = 0xFFE8E8E8;

    public static final int TEXT_PRIMARY = 0xFF111111;
    public static final int TEXT_SECONDARY = 0xFF666666;
    public static final int TEXT_DISABLED = 0xFF999999;

    public static final int ACCENT = 0xFF000000;
    public static final int ACCENT_HOVER = 0xFF333333;
    public static final int ACCENT_ACTIVE = 0xFF555555;

    public static final int BORDER = 0x18000000;
    public static final int BORDER_HOVER = 0x30000000;
    public static final int BORDER_ACTIVE = 0x50000000;

    public static final int TOGGLE_ON = 0xFF000000;
    public static final int TOGGLE_OFF = 0xFFCCCCCC;
    public static final int SLIDER_TRACK = 0xFFD0D0D0;
    public static final int SLIDER_FILL = 0xFF000000;

    public static final int GLASS_BG = 0xA6FFFFFF;
    public static final int GLASS_BORDER = 0x18000000;

    public static final float CORNER_RADIUS_SMALL = 4f;
    public static final float CORNER_RADIUS_MEDIUM = 8f;
    public static final float CORNER_RADIUS_LARGE = 12f;
    public static final float CORNER_RADIUS_XL = 16f;

    public static final float ANIMATION_SPEED_FAST = 0.25f;
    public static final float ANIMATION_SPEED_NORMAL = 0.15f;
    public static final float ANIMATION_SPEED_SLOW = 0.08f;

    public static int withAlpha(int color, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int blend(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF, a1 = (c1 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF, a2 = (c2 >> 24) & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        int a = (int) (a1 + (a2 - a1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
