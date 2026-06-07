package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockChangeEvent extends Event {
    private final BlockPos pos;
    private final BlockState oldState;
    private final BlockState newState;

    public BlockChangeEvent(BlockPos pos, BlockState oldState, BlockState newState) {
        this.pos = pos;
        this.oldState = oldState;
        this.newState = newState;
    }

    public BlockPos getPos() { return pos; }
    public BlockState getOldState() { return oldState; }
    public BlockState getNewState() { return newState; }
}
