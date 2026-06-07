package cn.gardenia.client.event.events.entity;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;

public class EntityDeathEvent extends Event {
    private final Entity entity;
    private final DamageSource damageSource;

    public EntityDeathEvent(Entity entity, DamageSource damageSource) {
        this.entity = entity;
        this.damageSource = damageSource;
    }

    public Entity getEntity() { return entity; }
    public DamageSource getDamageSource() { return damageSource; }
}
