package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;

public class BlockCollisionEvent extends Event {
    private final BlockPos pos;
    private final BlockState state;

    public BlockCollisionEvent(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
    }

    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }
}
