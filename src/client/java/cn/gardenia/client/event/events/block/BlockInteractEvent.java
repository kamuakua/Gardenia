package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;

public class BlockInteractEvent extends Event {
    private final BlockPos pos;
    private final Direction direction;

    public BlockInteractEvent(BlockPos pos, Direction direction) {
        this.pos = pos;
        this.direction = direction;
    }

    public BlockPos getPos() { return pos; }
    public Direction getDirection() { return direction; }
}
