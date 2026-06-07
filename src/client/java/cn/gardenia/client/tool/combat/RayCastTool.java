package cn.gardenia.client.tool.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.Optional;

public class RayCastTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public HitResult rayCast(Vec2f rotation, double range, float expand) {
        if (mc.player == null || mc.world == null || rotation == null) {
            return null;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = Vec3d.fromPolar(rotation.y, rotation.x);
        Vec3d endVec = eyePos.add(lookVec.multiply(range));
        BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                eyePos,
                endVec,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        HitResult entityHit = rayCastEntity(rotation, range, expand, true);
        if (!(entityHit instanceof EntityHitResult)) {
            return blockHit;
        }
        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
            return entityHit;
        }

        double entityDist = eyePos.squaredDistanceTo(entityHit.getPos());
        double blockDist = eyePos.squaredDistanceTo(blockHit.getPos());
        return entityDist < blockDist ? entityHit : blockHit;
    }

    public HitResult rayCastEntity(Vec2f rotation, double range, float expand, boolean ignoreBlocks) {
        if (!ignoreBlocks) {
            return rayCast(rotation, range, expand);
        }
        if (mc.player == null || mc.world == null || rotation == null) {
            return null;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = Vec3d.fromPolar(rotation.y, rotation.x);
        Vec3d endVec = eyePos.add(lookVec.multiply(range));
        Box searchBox = mc.player.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0D);

        Entity pointedEntity = null;
        Vec3d hitVec = null;
        double currentDist = range;

        List<Entity> candidates = mc.world.getOtherEntities(mc.player, searchBox, entity -> !entity.isSpectator() && entity.canHit());
        for (Entity candidate : candidates) {
            float collisionSize = candidate.getTargetingMargin() + expand;
            Box entityBox = candidate.getBoundingBox().expand(collisionSize);
            Optional<Vec3d> intercept = entityBox.raycast(eyePos, endVec);

            if (entityBox.contains(eyePos)) {
                if (currentDist >= 0.0D) {
                    pointedEntity = candidate;
                    hitVec = intercept.orElse(eyePos);
                    currentDist = 0.0D;
                }
            } else if (intercept.isPresent()) {
                Vec3d interceptVec = intercept.get();
                double distance = eyePos.distanceTo(interceptVec);
                if (distance < currentDist || currentDist == 0.0D) {
                    if (candidate.getRootVehicle() == mc.player.getRootVehicle() && !candidate.canHit()) {
                        if (currentDist == 0.0D) {
                            pointedEntity = candidate;
                            hitVec = interceptVec;
                        }
                    } else {
                        pointedEntity = candidate;
                        hitVec = interceptVec;
                        currentDist = distance;
                    }
                }
            }
        }

        return pointedEntity == null ? null : new EntityHitResult(pointedEntity, hitVec);
    }

    public Vec2f calculate(Entity target) {
        if (target == null || mc.player == null) {
            return new Vec2f(0.0F, 0.0F);
        }
        return calculateTo(target.getBoundingBox().getCenter());
    }

    public Vec2f calculateAdaptive(Entity target, double range) {
        if (target == null || mc.player == null) {
            return null;
        }

        Box box = target.getBoundingBox();
        Vec3d[] points = new Vec3d[]{
                box.getCenter(),
                new Vec3d((box.minX + box.maxX) * 0.5D, box.minY + target.getHeight() * 0.85D, (box.minZ + box.maxZ) * 0.5D),
                new Vec3d((box.minX + box.maxX) * 0.5D, box.minY + target.getHeight() * 0.65D, (box.minZ + box.maxZ) * 0.5D),
                new Vec3d((box.minX + box.maxX) * 0.5D, box.minY + target.getHeight() * 0.35D, (box.minZ + box.maxZ) * 0.5D)
        };

        for (Vec3d point : points) {
            Vec2f rotation = calculateTo(point);
            HitResult result = rayCast(rotation, range, 0.0F);
            if (result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target)) {
                return rotation;
            }
        }
        return null;
    }

    public Vec2f calculateTo(Vec3d target) {
        if (mc.player == null || target == null) {
            return new Vec2f(0.0F, 0.0F);
        }

        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Vec2f(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
    }
}
