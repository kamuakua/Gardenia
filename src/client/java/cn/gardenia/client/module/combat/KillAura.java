package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.event.events.render.Render3DEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.social.FriendManager;
import cn.gardenia.client.tool.ToolManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KillAura extends Module {
    public static KillAura INSTANCE;

    public Entity target;

    private final Setting<Double> aimRange = rangedDouble("Aim Range", "Rotation target range", 5.0D, 1.0D, 6.0D, 0.1D);
    private final Setting<Boolean> mode19 = new Setting<>("1.9 Mode", "Use 1.9 attack cooldown", false);
    private final Setting<Double> cps = rangedDouble("CPS", "Attacks per second", 10.0D, 1.0D, 20.0D, 1.0D);
    private final Setting<Double> rotateSpeed = rangedDouble("Rotation Speed", "Server rotation speed", 180.0D, 1.0D, 180.0D, 1.0D);
    private final Setting<Double> fov = rangedDouble("FOV", "Target field of view", 360.0D, 0.0D, 360.0D, 1.0D);

    private List<Entity> targets = new ArrayList<>();
    private long lastAttackTime = 0L;

    private final Consumer<PlayerTickEvent> tickListener = this::onPreTick;
    private final Consumer<Render3DEvent> renderListener = this::onRender;

    public KillAura() {
        super("KillAura", "Automatically attacks entities", Category.COMBAT);
        INSTANCE = this;
        addSetting(aimRange);
        addSetting(mode19);
        addSetting(cps);
        addSetting(rotateSpeed);
        addSetting(fov);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        target = null;
        targets = new ArrayList<>();
        lastAttackTime = 0L;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(Render3DEvent.class, renderListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(Render3DEvent.class, renderListener);
        target = null;
        targets.clear();
        ToolManager.INSTANCE.ROTATION.clearServerRotation();
    }

    public String getSuffix() {
        return targets.size() + " Targets";
    }

    private void onPreTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }

        findTarget();

        if (target != null) {
            Vec2f calculate = ToolManager.INSTANCE.ROTATION.calculate(target);
            ToolManager.INSTANCE.ROTATION.setServerRotation(calculate, rotateSpeed.getDouble(), rotation -> {
                HitResult hitResult = ToolManager.INSTANCE.RAY_CAST.rayCast(rotation, 3.0D, 0.0F);
                return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target);
            });

            HitResult hitResult = mc.crosshairTarget;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target)) {
                attackTarget();
            }
        } else {
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
        }
    }

    private void attackTarget() {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }

        if (mode19.getBoolean()) {
            if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                onPlayerAttack(target);
            }
            return;
        }

        long time = System.currentTimeMillis();
        double baseDelay = 1000.0D / Math.max(1.0D, cps.getDouble());
        long delay = (long) (baseDelay + (Math.random() - 0.5D) * baseDelay * 0.4D);

        if (time - lastAttackTime >= delay) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            onPlayerAttack(target);
            lastAttackTime = time;
        }
    }

    private void findTarget() {
        if (mc.player == null || mc.world == null) {
            target = null;
            targets = new ArrayList<>();
            return;
        }

        double range = aimRange.getDouble();
        double rangeSq = range * range;
        double currentFov = fov.getDouble();

        target = null;
        double minDstSq = Double.MAX_VALUE;

        Box searchBox = mc.player.getBoundingBox().expand(range);
        List<Entity> candidates = mc.world.getOtherEntities(
                mc.player,
                searchBox,
                entity -> entity instanceof LivingEntity
                        && entity != mc.player
                        && entity.isAlive()
                        && !entity.isSpectator()
                        && !AntiBots.isBot(entity)
                        && isTargetEnabled(entity)
                        && !Teams.isSameTeam(entity)
                        && !FriendManager.isFriend(entity)
                        && !isClientUser(entity)
                        && Math.abs(MathHelper.wrapDegrees(ToolManager.INSTANCE.ROTATION.calculate(entity).x - mc.player.getYaw())) <= currentFov
        );

        targets = candidates;

        for (Entity entity : candidates) {
            double distSq = mc.player.squaredDistanceTo(entity);
            if (distSq < minDstSq && distSq <= rangeSq) {
                minDstSq = distSq;
                target = entity;
            }
        }
    }

    public List<Entity> getTargets() {
        return targets;
    }

    public Entity getTarget() {
        return target;
    }

    public boolean filter(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return entity != mc.player
                && !entity.isRemoved()
                && living.isAlive()
                && !living.isSpectator()
                && !AntiBots.isBot(entity)
                && isTargetEnabled(entity)
                && !Teams.isSameTeam(entity)
                && !FriendManager.isFriend(entity)
                && !isClientUser(entity);
    }

    private boolean isTargetEnabled(Entity entity) {
        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        return targetModule == null || targetModule.isTarget(entity);
    }

    private boolean isClientUser(Entity entity) {
        return mc.player != null && entity instanceof PlayerEntity player && player.getUuid().equals(mc.player.getUuid());
    }

    private void onPlayerAttack(Entity entity) {
        // Gardenia does not currently have NavenAlpha's DynamicIslandHud hook.
    }

    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null || targets == null || targets.isEmpty()) {
            return;
        }
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return;
        }

        renderTargetBoxes();
    }

    private void renderTargetBoxes() {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.lineWidth(1.0F);

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.identity();

        Quaternionf camRot = camera.getRotation();
        Quaternionf invRot = new Quaternionf(camRot).conjugate();
        mvStack.rotate(camRot);

        VertexConsumerProvider.Immediate providers = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = providers.getBuffer(RenderLayer.LINES);
        MatrixStack matrices = new MatrixStack();

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            Vec3d entityPos = entity.getPos();
            Vector3f relVec = new Vector3f((float) (entityPos.x - camPos.x), (float) (entityPos.y - camPos.y), (float) (entityPos.z - camPos.z));
            relVec.rotate(invRot);

            matrices.push();
            matrices.translate(relVec.x(), relVec.y(), relVec.z());

            Box box = entity.getBoundingBox().offset(-entityPos.x, -entityPos.y, -entityPos.z);
            Color color = entity.equals(target) ? new Color(200, 0, 0, 60) : new Color(0, 200, 0, 60);
            VertexRendering.drawBox(
                    matrices,
                    lines,
                    box,
                    color.getRed() / 255.0F,
                    color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F,
                    color.getAlpha() / 255.0F
            );

            matrices.pop();
        }

        providers.draw(RenderLayer.LINES);
        mvStack.popMatrix();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
