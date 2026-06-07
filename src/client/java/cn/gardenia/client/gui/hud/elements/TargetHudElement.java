package cn.gardenia.client.gui.hud.elements;

import cn.gardenia.client.gui.hud.HudElement;
import cn.gardenia.client.gui.hud.HudStyle;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import net.minecraft.entity.player.PlayerEntity;

public class TargetHudElement extends HudElement {
    private HudStyle style = HudStyle.MODERN;
    private int textColor = 0xFFFFFFFF;
    private int accentColor = 0xFF888888;
    private float fontSize = 13;
    private float bgOpacity = 0.88f;
    private float padding = 14;
    private float cardRadius = 8;
    private PlayerEntity target;

    public TargetHudElement(float x, float y) {
        super(x, y, 180, 44);
    }

    public void setTarget(PlayerEntity target) {
        this.target = target;
    }

    @Override
    public void render(float mouseX, float mouseY, float delta, boolean editing) {
        update();
        if (target == null) return;

        String name = target.getDisplayName() != null ? target.getDisplayName().getString() : target.getName().getString();
        String healthStr = String.format("%.1f ❤", target.getHealth() / 2f);

        String font = NanoVGManager.INSTANCE.hasFont("chinese") ? "chinese" : "regular";
        float nameW = NanoVGRender.textWidth(name, fontSize, font);
        float healthW = NanoVGRender.textWidth(healthStr, fontSize - 2, font);
        float infoWidth = Math.max(nameW, healthW);
        width = padding + 36 + 8 + infoWidth + padding;
        height = 44;
        float alpha = editing ? 0.85f : 1.0f;

        if (style == HudStyle.MODERN) {
            NanoVGRender.drawShadow(x, y, width, height, cardRadius, 10, 0.15f * alpha);
            NanoVGRender.drawRoundedRect(x, y, width, height, cardRadius,
                    Theme.withAlpha(0xFF1A1A1A, bgOpacity * alpha));

            float avatarX = x + padding;
            float avatarY = y + (height - 24) / 2;
            NanoVGRender.drawRoundedRect(avatarX, avatarY, 24, 24, 6,
                    Theme.withAlpha(accentColor, 0.3f * alpha));
            NanoVGRender.drawText(name.substring(0, Math.min(1, name.length())),
                    avatarX + 12, avatarY + 12,
                    Theme.withAlpha(textColor, alpha), 14, "bold");

            float textX = avatarX + 32;
            NanoVGRender.drawText(name, textX, y + 14,
                    Theme.withAlpha(textColor, alpha), fontSize, font);
            NanoVGRender.drawText(healthStr, textX, y + 30,
                    Theme.withAlpha(getHealthColor(), alpha), fontSize - 2, font);
        } else {
            float avatarX = x + padding;
            float avatarY = y + (height - 24) / 2;

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

            NanoVGRender.drawRoundedRect(avatarX, avatarY, 24, 24, 6,
                    Theme.withAlpha(accentColor, 0.3f * alpha));
            NanoVGRender.drawText(name.substring(0, Math.min(1, name.length())),
                    avatarX + 12, avatarY + 12,
                    Theme.withAlpha(textColor, alpha), 14, "bold");

            float textX = avatarX + 32;
            NanoVGRender.drawText(name, textX, y + 14,
                    Theme.withAlpha(textColor, alpha), fontSize, font);
            NanoVGRender.drawText(healthStr, textX, y + 30,
                    Theme.withAlpha(getHealthColor(), alpha), fontSize - 2, font);
        }
    }

    private int getHealthColor() {
        if (target == null) return 0xFF00FF00;
        float health = target.getHealth() / target.getMaxHealth();
        if (health > 0.6f) return 0xFF00FF00;
        if (health > 0.3f) return 0xFFFFAA00;
        return 0xFFFF0000;
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
    public PlayerEntity getTarget() { return target; }
}
