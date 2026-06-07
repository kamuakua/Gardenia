package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class MovementEvent extends Event {
    private final MovementType type;
    private Vec3d movement;

    public MovementEvent(MovementType type, Vec3d movement) {
        this.type = type;
        this.movement = movement;
    }

    public MovementType getType() { return type; }
    public Vec3d getMovement() { return movement; }
    public void setMovement(Vec3d movement) { this.movement = movement; }
}
