package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class AttackEntityEvent extends Event {
    private final Entity target;
    private float damage;

    public AttackEntityEvent(Entity target, float damage) {
        this.target = target;
        this.damage = damage;
    }

    public Entity getTarget() { return target; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }
}
