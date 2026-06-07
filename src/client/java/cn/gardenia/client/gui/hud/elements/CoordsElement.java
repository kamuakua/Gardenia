package cn.gardenia.client.gui.hud.elements;

import cn.gardenia.client.gui.hud.HudElement;
import cn.gardenia.client.gui.hud.HudStyle;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;

public class CoordsElement extends HudElement {
    private HudStyle style = HudStyle.MODERN;
    private int textColor = 0xFFFFFFFF;
    private int accentColor = 0xFF888888;
    private float fontSize = 14;
    private float bgOpacity = 0.88f;
    private float padding = 14;
    private float accentWidth = 4;
    private float cardRadius = 8;
    private boolean showDirection = true;

    public CoordsElement(float x, float y) {
        super(x, y, 140, 24);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta, boolean editing) {
        update();
        if (mc.player == null) return;

        String coords = String.format("%.0f, %.0f, %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        String text = coords;
        if (showDirection) text += " §7[" + getDirection() + "]";

        String font = NanoVGManager.INSTANCE.hasFont("chinese") ? "chinese" : "regular";
        float textW = NanoVGRender.textWidth(text.replace("§7", ""), fontSize, font);
        width = padding + accentWidth + 6 + textW + padding;
        height = fontSize + 12;
        float alpha = editing ? 0.85f : 1.0f;

        if (style == HudStyle.MODERN) {
            NanoVGRender.drawShadow(x, y, width, height, cardRadius, 10, 0.15f * alpha);
            NanoVGRender.drawRoundedRect(x, y, width, height, cardRadius,
                    Theme.withAlpha(0xFF1A1A1A, bgOpacity * alpha));
            NanoVGRender.drawRoundedRect(x, y, accentWidth, height, 2,
                    Theme.withAlpha(accentColor, alpha));
            NanoVGRender.drawText(text, x + padding + accentWidth + 6, y + height / 2,
                    Theme.withAlpha(textColor, alpha), fontSize, font);
        } else {
            switch (style) {
                case GLASS -> {
                    NanoVGRender.drawGlassmorphism(x, y, width, height, cardRadius, bgOpacity * alpha);
                    NanoVGRender.drawRoundedRectStroke(x, y, width, height, cardRadius, 1,
                            Theme.withAlpha(accentColor, 0.3f * alpha));
                }
                case OUTLINE ->
                    NanoVGRender.drawRoundedRectStroke(x, y, width, height, cardRadius, 1,
                            Theme.withAlpha(accentColor, alpha));
                case BLOCK ->
                    NanoVGRender.drawRoundedRect(x, y, width, height, cardRadius,
                            Theme.withAlpha(accentColor, bgOpacity * alpha));
                case MINIMAL -> {}
                default -> {}
            }
            NanoVGRender.drawText(text, x + 12, y + height / 2,
                    Theme.withAlpha(textColor, alpha), fontSize, font);
        }
    }

    private String getDirection() {
        if (mc.player == null) return "?";
        float yaw = mc.player.getYaw();
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int idx = Math.round(yaw / 45f) % 8;
        if (idx < 0) idx += 8;
        return dirs[idx];
    }

    public void setStyle(HudStyle style) { this.style = style; }
    public HudStyle getStyle() { return style; }
    public void setTextColor(int color) { this.textColor = color; }
    public int getTextColor() { return textColor; }
    public void setAccentColor(int color) { this.accentColor = color; }
    public int getAccentColor() { return accentColor; }
    public void setFontSize(float size) { this.fontSize = size; }
    public float getFontSize() { return fontSize; }
    public void setBgOpacity(float opacity) { this.bgOpacity = opacity; }
    public float getBgOpacity() { return bgOpacity; }
    public void setShowDirection(boolean show) { this.showDirection = show; }
    public boolean isShowDirection() { return showDirection; }
}
