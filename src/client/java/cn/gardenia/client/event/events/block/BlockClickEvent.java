package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockClickEvent extends Event {
    private final BlockPos pos;
    private final BlockState state;
    private final int button;

    public BlockClickEvent(BlockPos pos, BlockState state, int button) {
        this.pos = pos;
        this.state = state;
        this.button = button;
    }

    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }
    public int getButton() { return button; }
    public boolean isLeftClick() { return button == 0; }
    public boolean isRightClick() { return button == 1; }
}
