package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;

public class StuckInBlockEvent extends Event {
    private final BlockState state;
    private Vec3d speedMultiplier;

    public StuckInBlockEvent(BlockState state, Vec3d speedMultiplier) {
        this.state = state;
        this.speedMultiplier = speedMultiplier;
    }

    public BlockState getState() {
        return state;
    }

    public Vec3d getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(Vec3d speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }
}
