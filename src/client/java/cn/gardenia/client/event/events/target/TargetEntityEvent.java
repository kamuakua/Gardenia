package cn.gardenia.client.event.events.target;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class TargetEntityEvent extends Event {
    private Entity target;

    public TargetEntityEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() { return target; }
    public void setTarget(Entity target) { this.target = target; }
}
