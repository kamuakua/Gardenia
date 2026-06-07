package cn.gardenia.client.event.events.entity;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class EntitySpawnEvent extends Event {
    private final Entity entity;

    public EntitySpawnEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() { return entity; }
}
