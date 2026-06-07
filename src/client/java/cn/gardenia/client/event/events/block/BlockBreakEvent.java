package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockBreakEvent extends Event {
    private final BlockPos pos;
    private final int breakProgress;
    private final Direction direction;

    public BlockBreakEvent(BlockPos pos, int breakProgress, Direction direction) {
        this.pos = pos;
        this.breakProgress = breakProgress;
        this.direction = direction;
    }

    public BlockPos getPos() { return pos; }
    public int getBreakProgress() { return breakProgress; }
    public Direction getDirection() { return direction; }
}
