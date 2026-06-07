package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.*;

import java.util.function.Predicate;

public class RotationTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Vec2f rotations;
    private Vec2f lastRotations;
    private Vec2f targetRotations;
    private Vec2f offset = new Vec2f(0.0F, 0.0F);
    private Predicate<Vec2f> raycast;
    private boolean active;
    private float randomAngle;

    public void init() {
        reset();
    }

    public float yaw() {
        if (mc.player == null) return 0;
        return mc.player.getYaw();
    }

    public float pitch() {
        if (mc.player == null) return 0;
        return mc.player.getPitch();
    }

    public void set(float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    public void setYaw(float yaw) {
        if (mc.player == null) return;
        mc.player.setYaw(yaw);
    }

    public void setPitch(float pitch) {
        if (mc.player == null) return;
        mc.player.setPitch(pitch);
    }

    public Vec2f calculate(Vec3d target) {
        if (mc.player == null) return new Vec2f(0, 0);
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        return new Vec2f(yaw, clampPitch(pitch));
    }

    public Vec2f calculate(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        return new Vec2f(yaw, clampPitch(pitch));
    }

    public Vec2f calculate(Entity target) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        return calculate(target.getBoundingBox().getCenter());
    }

    public Vec2f calculate(Entity target, Vec3d offset) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        return calculate(target.getPos().add(offset));
    }

    public Vec2f calculateBody(Entity target) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
        return calculate(targetPos);
    }

    public Vec2f calculateHead(Entity target) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.9, 0);
        return calculate(targetPos);
    }

    public Vec2f calculateFeet(Entity target) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        return calculate(target.getPos());
    }

    public Vec2f calculatePredictive(Entity target, double ticksAhead) {
        if (target == null || mc.player == null) return new Vec2f(0, 0);
        Vec3d predicted = target.getPos()
                .add(target.getVelocity().multiply(ticksAhead))
                .add(0, target.getHeight() * 0.75, 0);
        return calculate(predicted);
    }

    public Vec2f calculateBlock(BlockPos blockPos) {
        if (mc.player == null) return new Vec2f(0, 0);
        return calculate(Vec3d.ofCenter(blockPos));
    }

    public Vec2f calculateBlock(BlockPos blockPos, Direction side) {
        if (mc.player == null) return new Vec2f(0, 0);
        Vec3d pos = Vec3d.ofCenter(blockPos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        return calculate(pos);
    }

    public float wrapDegrees(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    public double wrapDegrees(double angle) {
        angle %= 360.0;
        if (angle >= 180.0) angle -= 360.0;
        if (angle < -180.0) angle += 360.0;
        return angle;
    }

    public float clampPitch(float pitch) {
        return Math.clamp(pitch, -90.0f, 90.0f);
    }

    public float angleDifference(float current, float target) {
        return wrapDegrees(target - current);
    }

    public double angleDifference(double current, double target) {
        return wrapDegrees(target - current);
    }

    public Vec2f smoothAngle(Vec2f current, Vec2f target, float speed) {
        float diffYaw = angleDifference(current.x, target.x);
        float diffPitch = angleDifference(current.y, target.y);

        float stepYaw = Math.clamp(diffYaw, -speed, speed);
        float stepPitch = Math.clamp(diffPitch, -speed, speed);

        return new Vec2f(current.x + stepYaw, clampPitch(current.y + stepPitch));
    }

    public float getYawTo(Vec3d target) {
        if (mc.player == null) return 0;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public float getYawDifference(Vec3d target) {
        return angleDifference(yaw(), getYawTo(target));
    }

    public float getPitchDifference(Vec3d target) {
        if (mc.player == null) return 0;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return angleDifference(pitch(), targetPitch);
    }

    public boolean isFacing(Vec3d target, float threshold) {
        float diff = Math.abs(getYawDifference(target));
        return diff <= threshold;
    }

    public boolean isFacing(Entity entity, float threshold) {
        if (entity == null) return false;
        return isFacing(entity.getBoundingBox().getCenter(), threshold);
    }

    public void setServerRotation(Vec2f rotation) {
        setServerRotation(rotation, 180.0);
    }

    public void setServerRotation(Vec2f rotation, double speed) {
        setServerRotation(rotation, speed, null);
    }

    public void setServerRotation(Vec2f rotation, double speed, Predicate<Vec2f> raycast) {
        if (rotation == null || mc.player == null) {
            return;
        }
        ensureState();
        targetRotations = rotation;
        this.raycast = raycast;
        rotations = smooth(lastRotations, targetRotations, speed, raycast);
        active = true;
    }

    public Vec2f getServerRotation() {
        if (active && rotations != null) {
            return rotations;
        }
        return mc.player == null ? new Vec2f(0.0F, 0.0F) : new Vec2f(mc.player.getYaw(), mc.player.getPitch());
    }

    public boolean isServerRotationActive() {
        return active && rotations != null;
    }

    public void tickServerRotation() {
        if (mc.player == null) {
            reset();
            return;
        }
        ensureState();
        if (active && targetRotations != null) {
            rotations = smooth(rotations, targetRotations, 180.0, raycast);
            lastRotations = rotations;
        } else {
            lastRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        }
        targetRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
    }

    public void clearServerRotation() {
        active = false;
        raycast = null;
        randomAngle = 0.0F;
        offset = new Vec2f(0.0F, 0.0F);
        if (mc.player != null) {
            lastRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            targetRotations = lastRotations;
            rotations = lastRotations;
        }
    }

    public PlayerMoveC2SPacket applyServerRotation(PlayerMoveC2SPacket packet) {
        if (!isServerRotationActive() || mc.player == null || packet == null) {
            return packet;
        }

        Vec2f rotation = getServerRotation();
        boolean onGround = packet.isOnGround();
        boolean horizontalCollision = packet.horizontalCollision();
        if (packet.changesPosition()) {
            return new PlayerMoveC2SPacket.Full(
                    packet.getX(mc.player.getX()),
                    packet.getY(mc.player.getY()),
                    packet.getZ(mc.player.getZ()),
                    rotation.x,
                    rotation.y,
                    onGround,
                    horizontalCollision
            );
        }
        return new PlayerMoveC2SPacket.LookAndOnGround(rotation.x, rotation.y, onGround, horizontalCollision);
    }

    public Vec2f fixMovement(float forward, float sideways, float yaw) {
        float directionFactor = Math.max(Math.abs(forward), Math.abs(sideways));
        if (directionFactor <= 0.0F || mc.player == null) {
            return new Vec2f(sideways, forward);
        }

        double angleDifference = wrapDegrees(movementDirection(forward, sideways) - yaw);
        double angleDistance = Math.abs(angleDifference);
        forward = 0.0F;
        sideways = 0.0F;

        float angleUnit = 45.0F;
        float angleTolerance = 22.5F;
        if (angleDistance <= angleUnit + angleTolerance) {
            forward++;
        } else if (angleDistance >= 180.0F - angleUnit - angleTolerance) {
            forward--;
        }

        if (angleDifference >= angleUnit - angleTolerance && angleDifference <= 180.0F - angleUnit + angleTolerance) {
            sideways--;
        } else if (angleDifference <= -angleUnit + angleTolerance && angleDifference >= -180.0F + angleUnit - angleTolerance) {
            sideways++;
        }

        return new Vec2f(sideways * directionFactor, forward * directionFactor);
    }

    private float movementDirection(float forward, float sideways) {
        float direction = mc.player.getYaw();
        boolean movingForward = forward > 0.0F;
        boolean movingBack = forward < 0.0F;
        boolean movingRight = sideways > 0.0F;
        boolean movingLeft = sideways < 0.0F;
        boolean movingSideways = movingRight || movingLeft;
        boolean movingStraight = movingForward || movingBack;

        if (forward != 0.0F || sideways != 0.0F) {
            if (movingBack && !movingSideways) {
                return direction + 180.0F;
            }
            if (movingForward && movingLeft) {
                return direction + 45.0F;
            }
            if (movingForward && movingRight) {
                return direction - 45.0F;
            }
            if (!movingStraight && movingLeft) {
                return direction + 90.0F;
            }
            if (!movingStraight && movingRight) {
                return direction - 90.0F;
            }
            if (movingBack && movingLeft) {
                return direction + 135.0F;
            }
            if (movingBack) {
                return direction - 135.0F;
            }
        }

        return direction;
    }

    private void ensureState() {
        if (mc.player == null) {
            return;
        }
        Vec2f current = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        if (rotations == null) rotations = current;
        if (lastRotations == null) lastRotations = current;
        if (targetRotations == null) targetRotations = current;
    }

    private Vec2f smooth(Vec2f from, Vec2f to, double speed) {
        return smooth(from, to, speed, null);
    }

    private Vec2f smooth(Vec2f from, Vec2f to, double speed, Predicate<Vec2f> raycast) {
        if (from == null || to == null || speed >= 180.0) {
            if (raycast == null || from == null || to == null || Math.abs(to.x - from.x) <= 5.0F && Math.abs(to.y - from.y) <= 5.0F) {
                return to;
            }
            return applyRaycastOffset(to, raycast);
        }
        Vec2f target = raycast == null || Math.abs(to.x - from.x) <= 5.0F && Math.abs(to.y - from.y) <= 5.0F
                ? to
                : applyRaycastOffset(to, raycast);
        return smoothAngle(from, target, (float) speed);
    }

    private Vec2f applyRaycastOffset(Vec2f target, Predicate<Vec2f> raycast) {
        double speed = Math.random() * Math.random() * Math.random() * 20.0D;
        randomAngle += (float) ((20.0F + (float) (Math.random() - 0.5D) * (Math.random() * Math.random() * Math.random() * 360.0D))
                * (mc.player == null || mc.player.age / 10 % 2 == 0 ? -1.0F : 1.0F));

        float offsetYaw = (float) (offset.x + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed);
        float offsetPitch = (float) (offset.y + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed);
        Vec2f candidate = new Vec2f(target.x + offsetYaw, target.y + offsetPitch);
        offset = new Vec2f(offsetYaw, offsetPitch);

        if (!raycast.test(candidate)) {
            randomAngle = (float) Math.toDegrees(Math.atan2(target.x - candidate.x, candidate.y - target.y)) - 180.0F;
            offsetYaw = (float) (offset.x + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed);
            offsetPitch = (float) (offset.y + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed);
            candidate = new Vec2f(target.x + offsetYaw, target.y + offsetPitch);
            offset = new Vec2f(offsetYaw, offsetPitch);
        }

        if (!raycast.test(candidate)) {
            offset = new Vec2f(0.0F, 0.0F);
            candidate = new Vec2f((float) (target.x + Math.random() * 2.0D), (float) (target.y + Math.random() * 2.0D));
        }

        return candidate;
    }

    private void reset() {
        active = false;
        rotations = null;
        lastRotations = null;
        targetRotations = null;
    }
}
