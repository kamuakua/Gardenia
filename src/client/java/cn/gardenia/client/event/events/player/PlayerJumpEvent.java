package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerJumpEvent extends Event {
    private float velocity;

    public PlayerJumpEvent(float velocity) {
        this.velocity = velocity;
    }

    public float getVelocity() { return velocity; }
    public void setVelocity(float velocity) { this.velocity = velocity; }
}
