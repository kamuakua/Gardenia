package cn.gardenia.client.event.events.entity;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class EntityInteractEvent extends Event {
    private final Entity target;
    private final boolean offHand;

    public EntityInteractEvent(Entity target, boolean offHand) {
        this.target = target;
        this.offHand = offHand;
    }

    public Entity getTarget() { return target; }
    public boolean isOffHand() { return offHand; }
}
