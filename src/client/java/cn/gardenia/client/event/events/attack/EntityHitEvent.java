package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class EntityHitEvent extends Event {
    private final Entity target;
    private final float damage;
    private final boolean pre;

    public EntityHitEvent(Entity target, float damage, boolean pre) {
        this.target = target;
        this.damage = damage;
        this.pre = pre;
    }

    public Entity getTarget() { return target; }
    public float getDamage() { return damage; }
    public boolean isPre() { return pre; }
    public boolean isPost() { return !pre; }
}
