package cn.gardenia.client.event.events.input;

import cn.gardenia.client.event.Event;

public class MouseScrollEvent extends Event {
    private final double horizontal;
    private final double vertical;
    private final double mouseX;
    private final double mouseY;

    public MouseScrollEvent(double horizontal, double vertical, double mouseX, double mouseY) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public double getHorizontal() { return horizontal; }
    public double getVertical() { return vertical; }
    public double getX() { return mouseX; }
    public double getY() { return mouseY; }
}
