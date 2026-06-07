package cn.gardenia.client.tool.block;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

public class BlockTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public net.minecraft.block.BlockState getState(BlockPos pos) {
        if (mc.world == null) return null;
        return mc.world.getBlockState(pos);
    }

    public net.minecraft.block.Block getBlock(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        return state != null ? state.getBlock() : null;
    }

    public boolean isAir(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        return state == null || state.isAir();
    }

    public boolean isReplaceable(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        return state == null || state.isReplaceable();
    }

    public boolean isSolid(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        return state != null && state.isSolid();
    }

    public boolean isLiquid(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        return state != null && !state.getFluidState().isEmpty();
    }

    public float getHardness(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        if (state == null || mc.player == null) return -1;
        return state.getHardness(mc.world, pos);
    }

    public float getDestroyProgress(BlockPos pos) {
        net.minecraft.block.BlockState state = getState(pos);
        if (state == null || mc.player == null) return 0;
        return mc.player.getBlockBreakingSpeed(state);
    }

    public BlockHitResult raycast(double range) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(range)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    public BlockHitResult raycastFluid(double range) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(range)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                mc.player
        ));
    }

    public BlockHitResult raycast(double range, RaycastContext.ShapeType shape, RaycastContext.FluidHandling fluid) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(range)),
                shape,
                fluid,
                mc.player
        ));
    }

    public boolean breakBlock(BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return false;
        if (!canReach(pos)) return false;
        mc.interactionManager.attackBlock(pos, getClosestDirection(pos));
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        return true;
    }

    public boolean breakBlockSilent(BlockPos pos) {
        if (mc.interactionManager == null || mc.player == null) return false;
        if (!canReach(pos)) return false;
        mc.interactionManager.attackBlock(pos, getClosestDirection(pos));
        return true;
    }

    public boolean breakBlockPacket(BlockPos pos) {
        if (mc.getNetworkHandler() == null || mc.player == null) return false;
        if (!canReach(pos)) return false;
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(
                net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                pos,
                getClosestDirection(pos)
        ));
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        return true;
    }

    public boolean interact(BlockPos pos, Direction side) {
        if (mc.interactionManager == null || mc.player == null) return false;
        if (!canReach(pos, side)) return false;
        Vec3d hitPos = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitPos, side, pos, false));
        return true;
    }

    public boolean canReach(BlockPos pos) {
        if (mc.player == null) return false;
        double reach = mc.interactionManager != null
                && mc.interactionManager.getCurrentGameMode() == net.minecraft.world.GameMode.CREATIVE ? 6.0 : 5.0;
        return mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= reach * reach;
    }

    public boolean canReach(BlockPos pos, Direction side) {
        Vec3d targetPos = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        if (mc.player == null) return false;
        double reach = mc.interactionManager != null
                && mc.interactionManager.getCurrentGameMode() == net.minecraft.world.GameMode.CREATIVE ? 6.0 : 5.0;
        return mc.player.squaredDistanceTo(targetPos) <= reach * reach;
    }

    public Direction getClosestDirection(BlockPos pos) {
        if (mc.player == null) return Direction.UP;
        Vec3d playerPos = mc.player.getPos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = playerPos.subtract(blockCenter);

        Direction closest = Direction.UP;
        double closestDist = Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            double dist = Math.abs(diff.x * dir.getOffsetX()
                    + diff.y * dir.getOffsetY()
                    + diff.z * dir.getOffsetZ());
            if (dist < closestDist) {
                closestDist = dist;
                closest = dir;
            }
        }
        return closest.getOpposite();
    }

    public Direction getDirection(Vec3d hitPos, BlockPos blockPos) {
        Vec3d center = Vec3d.ofCenter(blockPos);
        Vec3d diff = hitPos.subtract(center);

        double ax = Math.abs(diff.x);
        double ay = Math.abs(diff.y);
        double az = Math.abs(diff.z);

        if (ax >= ay && ax >= az) {
            return diff.x > 0 ? Direction.EAST : Direction.WEST;
        } else if (ay >= ax && ay >= az) {
            return diff.y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public BlockPos getRelativeBlock(BlockPos pos, Direction dir) {
        return pos.offset(dir);
    }

    public int getTotalHardness(BlockPos pos) {
        if (mc.world == null) return -1;
        float hardness = mc.world.getBlockState(pos).getHardness(mc.world, pos);
        if (hardness < 0) return -1;
        return Math.round(1.5f / hardness);
    }
}
