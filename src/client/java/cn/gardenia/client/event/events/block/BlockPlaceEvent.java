package cn.gardenia.client.event.events.block;

import cn.gardenia.client.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.item.ItemStack;

public class BlockPlaceEvent extends Event {
    private final BlockPos pos;
    private final Direction direction;
    private final ItemStack stack;
    private final BlockHitResult hitResult;

    public BlockPlaceEvent(BlockPos pos, Direction direction, ItemStack stack, BlockHitResult hitResult) {
        this.pos = pos;
        this.direction = direction;
        this.stack = stack;
        this.hitResult = hitResult;
    }

    public BlockPos getPos() { return pos; }
    public Direction getDirection() { return direction; }
    public ItemStack getStack() { return stack; }
    public BlockHitResult getHitResult() { return hitResult; }
}
