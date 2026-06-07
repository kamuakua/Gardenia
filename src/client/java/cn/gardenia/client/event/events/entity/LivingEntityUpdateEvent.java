package cn.gardenia.client.event.events.entity;

import cn.gardenia.client.event.Event;

public class LivingEntityUpdateEvent extends Event {
    private final int entityId;

    public LivingEntityUpdateEvent(int entityId) {
        this.entityId = entityId;
    }

    public int getEntityId() { return entityId; }
}
