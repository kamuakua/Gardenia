package cn.gardenia.client.event.events.target;

import cn.gardenia.client.event.Event;
import net.minecraft.util.math.BlockPos;

public class TargetBlockEvent extends Event {
    private BlockPos target;

    public TargetBlockEvent(BlockPos target) {
        this.target = target;
    }

    public BlockPos getTarget() { return target; }
    public void setTarget(BlockPos target) { this.target = target; }
}
