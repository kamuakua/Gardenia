package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerTravelEvent extends Event {
    private final float strafe;
    private final float forward;
    private final float vertical;

    public PlayerTravelEvent(float strafe, float forward, float vertical) {
        this.strafe = strafe;
        this.forward = forward;
        this.vertical = vertical;
    }

    public float getStrafe() { return strafe; }
    public float getForward() { return forward; }
    public float getVertical() { return vertical; }
}
