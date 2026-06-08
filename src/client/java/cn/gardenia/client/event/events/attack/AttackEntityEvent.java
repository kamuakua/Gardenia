package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;
import cn.gardenia.client.event.EventPhase;
import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Event {
    private final Entity target;
    private final EventPhase phase;
    private float damage;

    public AttackEntityEvent(Entity target, float damage) {
        this(target, damage, EventPhase.PRE);
    }

    public AttackEntityEvent(Entity target, float damage, EventPhase phase) {
        this.target = target;
        this.damage = damage;
        this.phase = phase;
    }

    public Entity getTarget() { return target; }
    public EventPhase getPhase() { return phase; }
    public boolean isPre() { return phase == EventPhase.PRE; }
    public boolean isPost() { return phase == EventPhase.POST; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }
}
