package cn.gardenia.client.tool.block;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public class PlacementTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum BuildMode {
        NORMAL,
        SILENT,
        PACKET
    }

    private BuildMode mode = BuildMode.NORMAL;
    private boolean rotate = true;

    public void init() {}

    public void setMode(BuildMode mode) { this.mode = mode; }
    public void setRotate(boolean rotate) { this.rotate = rotate; }

    public boolean place(BlockPos pos, Direction side) {
        return place(pos, side, Hand.MAIN_HAND);
    }

    public boolean place(BlockPos pos, Direction side, Hand hand) {
        if (mc.player == null || mc.interactionManager == null) return false;

        Vec3d hitPos = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );

        if (rotate) {
            Vec3d eyePos = mc.player.getEyePos();
            double dx = hitPos.x - eyePos.x;
            double dy = hitPos.y - eyePos.y;
            double dz = hitPos.z - eyePos.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
            mc.player.setYaw(yaw);
            mc.player.setPitch(Math.clamp(pitch, -90.0f, 90.0f));
        }

        BlockHitResult hit = new BlockHitResult(hitPos, side, pos, false);

        return switch (mode) {
            case NORMAL -> {
                mc.interactionManager.interactBlock(mc.player, hand, hit);
                mc.player.swingHand(hand);
                yield true;
            }
            case SILENT -> {
                mc.interactionManager.interactBlock(mc.player, hand, hit);
                yield true;
            }
            case PACKET -> {
                mc.getNetworkHandler().sendPacket(
                        new net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket(hand, hit, 0)
                );
                mc.player.swingHand(hand);
                yield true;
            }
        };
    }

    public boolean placeOnTop(BlockPos pos, Hand hand) {
        return place(pos.up(), Direction.DOWN, hand);
    }

    public BlockPos getScaffoldPos() {
        if (mc.player == null) return null;
        Vec3d pos = mc.player.getPos();
        BlockPos playerPos = mc.player.getBlockPos();
        Vec3d forward = mc.player.getRotationVec(1.0f);

        BlockPos target = playerPos.down();
        if (isAirOrReplaceable(target)) {
            return findClosestSupport(playerPos);
        }

        double fwdX = forward.x;
        double fwdZ = forward.z;
        double absX = Math.abs(fwdX);
        double absZ = Math.abs(fwdZ);

        Direction dir;
        if (absX > absZ) {
            dir = fwdX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            dir = fwdZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        BlockPos forwardPos = playerPos.offset(dir);
        if (isAirOrReplaceable(forwardPos.down())) {
            return forwardPos.down();
        }

        return null;
    }

    public BlockPos getScaffoldPos(double range) {
        if (mc.player == null) return null;
        BlockPos base = mc.player.getBlockPos().down();
        if (!isAirOrReplaceable(base)) return base;

        BlockPos best = null;
        double bestDist = range * range;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos candidate = base.add(x, 0, z);
                if (isAirOrReplaceable(candidate)) continue;
                double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(candidate));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = candidate.up();
                }
            }
        }
        return best;
    }

    public Direction getPlaceDirection(BlockPos pos) {
        if (mc.player == null) return Direction.UP;
        Vec3d playerPos = mc.player.getPos();
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d diff = playerPos.subtract(center);

        Direction best = Direction.UP;
        double bestDot = Double.NEGATIVE_INFINITY;

        for (Direction dir : Direction.values()) {
            double dot = diff.dotProduct(Vec3d.of(dir.getVector()));
            if (dot > bestDot) {
                bestDot = dot;
                best = dir;
            }
        }
        return best;
    }

    public boolean isAirOrReplaceable(BlockPos pos) {
        if (mc.world == null) return true;
        var state = mc.world.getBlockState(pos);
        return state.isAir() || state.isReplaceable();
    }

    private BlockPos findClosestSupport(BlockPos playerPos) {
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    BlockPos check = playerPos.add(dx, -1, dz);
                    if (!isAirOrReplaceable(check)) {
                        return check.up();
                    }
                }
            }
        }
        return null;
    }

    public BlockHitResult getPlacementHit(BlockPos pos, Direction side) {
        Vec3d hitPos = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        return new BlockHitResult(hitPos, side, pos, false);
    }

    public Direction getSideForPlacement(BlockPos target, BlockPos support) {
        Vec3d diff = Vec3d.of(target.subtract(support));
        if (diff.x > 0) return Direction.EAST;
        if (diff.x < 0) return Direction.WEST;
        if (diff.y > 0) return Direction.UP;
        if (diff.y < 0) return Direction.DOWN;
        if (diff.z > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }
}
