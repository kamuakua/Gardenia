package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerHealthEvent extends Event {
    private final float health;
    private final float maxHealth;
    private final float absorption;

    public PlayerHealthEvent(float health, float maxHealth, float absorption) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.absorption = absorption;
    }

    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getAbsorption() { return absorption; }
}
