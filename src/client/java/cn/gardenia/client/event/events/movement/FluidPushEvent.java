package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;

public class FluidPushEvent extends Event {
    private double horizontalPush;
    private double verticalPush;

    public FluidPushEvent(double horizontalPush, double verticalPush) {
        this.horizontalPush = horizontalPush;
        this.verticalPush = verticalPush;
    }

    public double getHorizontalPush() { return horizontalPush; }
    public double getVerticalPush() { return verticalPush; }
    public void setHorizontalPush(double h) { this.horizontalPush = h; }
    public void setVerticalPush(double v) { this.verticalPush = v; }
}
