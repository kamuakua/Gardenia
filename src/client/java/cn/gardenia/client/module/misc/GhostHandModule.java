package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.input.ClickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class GhostHandModule extends Module {
    private static final Set<Block> BLOCKS = new HashSet<>();

    static {
        BLOCKS.add(Blocks.CHEST);
        BLOCKS.add(Blocks.ENDER_CHEST);
        BLOCKS.add(Blocks.TRAPPED_CHEST);
        BLOCKS.add(Blocks.SHULKER_BOX);
    }

    private final Consumer<ClickEvent> clickListener = this::onClick;

    public GhostHandModule() {
        super("GhostHand", "Allows interacting with blocks through walls", Category.MISC);
    }

    @Override
    public void onEnable() {
        subscribe(ClickEvent.class, clickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(ClickEvent.class, clickListener);
    }

    private void onClick(ClickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || !mc.options.useKey.isPressed()) {
            return;
        }
        if (ghostInteractWithContainer()) {
            event.cancel();
        }
    }

    private boolean ghostInteractWithContainer() {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d reachEnd = eyePos.add(mc.player.getRotationVec(1.0F).multiply(4.5D));

        BlockPos origin = BlockPos.ofFloored(eyePos);
        BlockHitResult bestHit = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(origin.add(-5, -5, -5), origin.add(5, 5, 5))) {
            BlockState state = mc.world.getBlockState(pos);
            if (!isTargetBlock(state.getBlock())) {
                continue;
            }

            Box box = state.getOutlineShape(mc.world, pos).getBoundingBox().offset(pos);
            Optional<Vec3d> hit = box.raycast(eyePos, reachEnd);
            if (hit.isEmpty()) {
                continue;
            }

            double distance = hit.get().distanceTo(eyePos);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestHit = new BlockHitResult(hit.get(), Direction.UP, pos.toImmutable(), false);
            }
        }

        if (bestHit == null) {
            return false;
        }

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bestHit);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean isTargetBlock(Block block) {
        return BLOCKS.contains(block) || block instanceof ChestBlock || block instanceof ShulkerBoxBlock;
    }
}
