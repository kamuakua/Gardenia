package cn.gardenia.client.event.events.input;

import cn.gardenia.client.event.Event;

public class MouseEvent extends Event {
    private final int button;
    private final int action;
    private final int modifiers;
    private final double mouseX;
    private final double mouseY;

    public static final int CLICK = 1;
    public static final int RELEASE = 0;

    public MouseEvent(int button, int action, int modifiers, double mouseX, double mouseY) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public int getButton() { return button; }
    public int getAction() { return action; }
    public int getModifiers() { return modifiers; }
    public double getX() { return mouseX; }
    public double getY() { return mouseY; }
    public boolean isClick() { return action == CLICK; }
    public boolean isRelease() { return action == RELEASE; }
}
