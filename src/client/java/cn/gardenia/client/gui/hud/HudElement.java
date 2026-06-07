package cn.gardenia.client.gui.hud;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import net.minecraft.client.MinecraftClient;

public abstract class HudElement {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    protected float x, y;
    protected float width, height;
    protected boolean visible = true;
    protected boolean dragging = false;
    protected boolean editing = false;
    protected boolean hovered = false;
    protected float dragOffsetX, dragOffsetY;
    protected Animation hoverAnimation;
    protected Animation editAnimation;
    protected Animation alphaAnimation;

    public HudElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hoverAnimation = new Animation(0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_QUAD);
        this.editAnimation = new Animation(0, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_CUBIC);
        this.alphaAnimation = new Animation(1, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_QUAD);
    }

    public abstract void render(float mouseX, float mouseY, float delta, boolean editing);

    public void update() {
        hoverAnimation.update();
        editAnimation.update();
        alphaAnimation.update();
    }

    public void renderEditOverlay(float mouseX, float mouseY) {
        if (!editing) return;
        float editAlpha = editAnimation.getCurrent();
        if (editAlpha < 0.01f) return;

        int borderColor = Theme.withAlpha(Theme.ACCENT, 0.5f * editAlpha);
        NanoVGRender.drawShadow(x - 3, y - 3, width + 6, height + 6, 8, 8, 0.12f * editAlpha);
        NanoVGRender.drawRoundedRectStroke(x - 2, y - 2, width + 4, height + 4, 8, 1.5f, borderColor);

        float handleSize = 5;
        int handleColor = Theme.withAlpha(Theme.ACCENT, editAlpha * 0.8f);
        NanoVGRender.drawCircle(x, y, handleSize, handleColor);
        NanoVGRender.drawCircle(x + width, y, handleSize, handleColor);
        NanoVGRender.drawCircle(x, y + height, handleSize, handleColor);
        NanoVGRender.drawCircle(x + width, y + height, handleSize, handleColor);

        if (hovered || dragging) {
            NanoVGRender.drawRoundedRect(x, y, width, height, 6,
                    Theme.withAlpha(Theme.ACCENT, 0.06f * editAlpha));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !editing) return false;
        if (isMouseOver(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (float) (mouseX - x);
                dragOffsetY = (float) (mouseY - y);
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !editing) return false;
        if (dragging && button == 0) {
            float sw = mc.getWindow().getScaledWidth();
            float sh = mc.getWindow().getScaledHeight();
            x = (float) Math.max(0, Math.min(sw - width, mouseX - dragOffsetX));
            y = (float) Math.max(0, Math.min(sh - height, mouseY - dragOffsetY));
            return true;
        }
        return false;
    }

    public void onMouseMove(double mouseX, double mouseY) {
        boolean wasHovered = hovered;
        hovered = isMouseOver(mouseX, mouseY);
        if (hovered != wasHovered) {
            hoverAnimation.setTarget(hovered ? 1 : 0);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        this.editAnimation.setTarget(editing ? 1 : 0);
        if (!editing) {
            dragging = false;
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEditing() { return editing; }
    public boolean isDragging() { return dragging; }
}
