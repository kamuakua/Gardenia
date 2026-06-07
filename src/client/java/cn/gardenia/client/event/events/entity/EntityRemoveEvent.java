package cn.gardenia.client.event.events.entity;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class EntityRemoveEvent extends Event {
    private final boolean dead;
    private final Entity entity;

    public EntityRemoveEvent(Entity entity) {
        this(false, entity);
    }

    public EntityRemoveEvent(boolean dead, Entity entity) {
        this.dead = dead;
        this.entity = entity;
    }

    public boolean isDead() { return dead; }
    public Entity getEntity() { return entity; }
}
