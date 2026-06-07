package cn.gardenia.client.event.events.input;

import cn.gardenia.client.event.Event;

public class MouseDragEvent extends Event {
    private final int button;
    private final double mouseX;
    private final double mouseY;
    private final double deltaX;
    private final double deltaY;

    public MouseDragEvent(int button, double mouseX, double mouseY, double deltaX, double deltaY) {
        this.button = button;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public int getButton() { return button; }
    public double getX() { return mouseX; }
    public double getY() { return mouseY; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
}
