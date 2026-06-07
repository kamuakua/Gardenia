package cn.gardenia.client.tool.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

public class CameraTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public Camera getCamera() {
        return mc.gameRenderer.getCamera();
    }

    public Vec3d getPos() {
        return getCamera().getPos();
    }

    public Vec2f getRotation() {
        Camera camera = getCamera();
        return new Vec2f(camera.getYaw(), camera.getPitch());
    }

    public float getYaw() {
        return getCamera().getYaw();
    }

    public float getPitch() {
        return getCamera().getPitch();
    }

    public Vec3d getForward() {
        float yaw = (float)Math.toRadians(getYaw());
        float pitch = (float)Math.toRadians(getPitch());
        return new Vec3d(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        );
    }

    public Vec3d getRight() {
        float yaw = (float)Math.toRadians(getYaw());
        return new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
    }

    public Vec3d getUp() {
        return getForward().crossProduct(getRight()).normalize();
    }

    public Frustum getFrustum() {
        return mc.worldRenderer.getCapturedFrustum();
    }

    public boolean isVisible(Box box) {
        Frustum frustum = getFrustum();
        return frustum != null && frustum.isVisible(box);
    }

    public boolean isVisible(Vec3d pos, double width, double height) {
        double hw = width / 2.0;
        return isVisible(new Box(pos.x - hw, pos.y, pos.z - hw, pos.x + hw, pos.y + height, pos.z + hw));
    }

    public Vec3d screenToWorld(double screenX, double screenY) {
        Camera camera = getCamera();
        Vec3d camPos = camera.getPos();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();

        double fov = mc.options.getFov().getValue();
        double aspect = mc.getWindow().getWidth() / (double)mc.getWindow().getHeight();

        double ndcX = (screenX / mc.getWindow().getWidth()) * 2.0 - 1.0;
        double ndcY = (screenY / mc.getWindow().getHeight()) * 2.0 - 1.0;

        double tanFov = Math.tan(Math.toRadians(fov) / 2.0);

        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        Vec3d forward = new Vec3d(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
        Vec3d right = new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad));
        Vec3d up = forward.crossProduct(right).normalize();

        Vec3d dir = new Vec3d(0, 0, 0)
                .add(forward)
                .add(right.multiply(ndcX * tanFov * aspect))
                .add(up.multiply(-ndcY * tanFov))
                .normalize();

        return camPos.add(dir.multiply(100.0));
    }

    public RaycastResult raycast(double maxDist) {
        Vec3d origin = getPos();
        Vec3d dir = getForward();
        if (mc.world == null) return null;

        HitResult hit = mc.world.raycast(new RaycastContext(
                origin,
                origin.add(dir.multiply(maxDist)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return new RaycastResult(
                    hit.getPos(),
                    ((BlockHitResult) hit).getBlockPos(),
                    ((BlockHitResult) hit).getSide(),
                    hit
            );
        }
        return null;
    }

    public static class RaycastResult {
        public final Vec3d hitPos;
        public final BlockPos blockPos;
        public final Direction side;
        public final HitResult hit;

        public RaycastResult(Vec3d hitPos, BlockPos blockPos, Direction side, HitResult hit) {
            this.hitPos = hitPos;
            this.blockPos = blockPos;
            this.side = side;
            this.hit = hit;
        }
    }
}
