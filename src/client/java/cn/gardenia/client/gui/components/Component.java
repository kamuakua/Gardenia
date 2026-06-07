package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.theme.Theme;

public abstract class Component {
    protected float x, y, width, height;
    protected boolean visible = true;
    protected boolean hovered = false;
    protected boolean focused = false;
    protected Animation hoverAnimation;
    protected Animation alphaAnimation;
    protected Animation scaleAnimation;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hoverAnimation = new Animation(0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.alphaAnimation = new Animation(1, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_QUAD);
        this.scaleAnimation = new Animation(1, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_BACK);
    }

    public abstract void render(float mouseX, float mouseY, float delta);

    public void update() {
        hoverAnimation.update();
        alphaAnimation.update();
        scaleAnimation.update();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        onClick(mouseX, mouseY, button);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        onRelease(mouseX, mouseY, button);
        return isMouseOver(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        onDrag(mouseX, mouseY, button, deltaX, deltaY);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        onScroll(mouseX, mouseY, amount);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !focused) return false;
        onKeyPress(keyCode, scanCode, modifiers);
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !focused) return false;
        onCharTyped(chr, modifiers);
        return true;
    }

    protected void onClick(double mouseX, double mouseY, int button) {}
    protected void onRelease(double mouseX, double mouseY, int button) {}
    protected void onDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
    protected void onScroll(double mouseX, double mouseY, double amount) {}
    protected void onKeyPress(int keyCode, int scanCode, int modifiers) {}
    protected void onCharTyped(char chr, int modifiers) {}

    public void onMouseMove(double mouseX, double mouseY) {
        boolean wasHovered = hovered;
        hovered = isMouseOver(mouseX, mouseY);
        if (hovered != wasHovered) {
            hoverAnimation.setTarget(hovered ? 1 : 0);
            if (hovered) {
                onHoverEnter();
            } else {
                onHoverLeave();
            }
        }
    }

    protected void onHoverEnter() {}
    protected void onHoverLeave() {}

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isMouseOver(double mouseX, double mouseY, float cx, float cy, float cw, float ch) {
        return mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isHovered() { return hovered; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    public float getHoverProgress() { return hoverAnimation.getCurrent(); }
    public float getAlpha() { return alphaAnimation.getCurrent(); }
    public float getScale() { return scaleAnimation.getCurrent(); }
}
