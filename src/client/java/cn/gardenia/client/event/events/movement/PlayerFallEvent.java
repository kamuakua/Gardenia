package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;

public class PlayerFallEvent extends Event {
    private final float distance;
    private final float damageMultiplier;

    public PlayerFallEvent(float distance, float damageMultiplier) {
        this.distance = distance;
        this.damageMultiplier = damageMultiplier;
    }

    public float getDistance() { return distance; }
    public float getDamageMultiplier() { return damageMultiplier; }
}
