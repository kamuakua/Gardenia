package cn.gardenia.client.tool.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;

public class ReachTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public double getAttackReach() {
        if (mc.player == null) return 4.5;
        if (mc.interactionManager == null) return 4.5;
        return mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE ? 6.0 : 4.5;
    }

    public double getBlockReach() {
        if (mc.player == null) return 5.0;
        if (mc.interactionManager == null) return 5.0;
        return mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE ? 6.0 : 5.0;
    }

    public boolean canReach(Entity entity) {
        if (mc.player == null || entity == null) return false;
        return mc.player.distanceTo(entity) <= getAttackReach()
                && mc.player.canSee(entity);
    }

    public boolean canReach(BlockPos pos) {
        if (mc.player == null || pos == null) return false;
        return mc.player.getBlockPos().getManhattanDistance(pos) <= getBlockReach()
                && mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= getBlockReach() * getBlockReach();
    }

    public boolean canReach(BlockPos pos, Direction side) {
        Vec3d targetPos = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        if (mc.player == null) return false;
        return mc.player.squaredDistanceTo(targetPos) <= getBlockReach() * getBlockReach();
    }

    public double getDistanceTo(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(entity);
    }

    public double getSquaredDistanceTo(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        return mc.player.squaredDistanceTo(entity);
    }

    public Vec3d getEyePos() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getEyePos();
    }

    public EntityHitResult raycastEntity(double range) {
        if (mc.world == null || mc.player == null) return null;

        Vec3d start = mc.player.getEyePos();
        Vec3d end = start.add(mc.player.getRotationVec(1.0f).multiply(range));
        Box searchBox = mc.player.getBoundingBox().stretch(
                mc.player.getRotationVec(1.0f).multiply(range)
        ).expand(1.0, 1.0, 1.0);

        Entity closest = null;
        double closestDist = range * range;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            if (!(e instanceof PlayerEntity) && !(e instanceof net.minecraft.entity.LivingEntity)) continue;

            Box box = e.getBoundingBox().expand(e.getTargetingMargin());
            Vec3d hitPos = box.raycast(start, end).orElse(null);
            if (hitPos == null) continue;

            double dist = start.squaredDistanceTo(hitPos);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }

        if (closest == null) return null;
        return new EntityHitResult(closest);
    }

    public BlockHitResult raycastBlock(double range) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(range)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    public BlockHitResult raycastBlock(double range, RaycastContext.FluidHandling fluidHandling) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(range)),
                RaycastContext.ShapeType.OUTLINE,
                fluidHandling,
                mc.player
        ));
    }

    public HitResult raycastAll(double range) {
        BlockHitResult blockHit = raycastBlock(range);
        EntityHitResult entityHit = raycastEntity(range);

        if (blockHit == null && entityHit == null) return null;
        if (blockHit == null) return entityHit;
        if (entityHit == null) return blockHit;

        Vec3d start = mc.player.getEyePos();
        double blockDist = start.squaredDistanceTo(blockHit.getPos());
        double entityDist = start.squaredDistanceTo(entityHit.getEntity().getPos());

        return entityDist < blockDist ? entityHit : blockHit;
    }

    public Vec3d getLookVector() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getRotationVec(1.0f);
    }

    public boolean shouldSprintReset() {
        if (mc.player == null) return false;
        return mc.player.getAttackCooldownProgress(0) >= 0.9f;
    }
}
