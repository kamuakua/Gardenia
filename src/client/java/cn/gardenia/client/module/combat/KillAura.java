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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class KillAura extends Module {
    public static KillAura INSTANCE;

    private Entity target;
    private Entity lastTarget;

    // 基础设置
    private final Setting<Double> attackRange = rangedDouble("Attack Range", "实际攻击范围", 3.2D, 3.0D, 6.0D, 0.1D);
    private final Setting<Double> scanRange = rangedDouble("Scan Range", "搜索目标范围", 5.0D, 3.0D, 8.0D, 0.1D);
    private final Setting<Boolean> mode19 = new Setting<>("1.9 Mode", "使用1.9攻击冷却", false);
    private final Setting<Double> cps = rangedDouble("CPS", "每秒攻击次数", 12.0D, 1.0D, 20.0D, 0.5D);

    // 旋转设置
    private final Setting<Double> rotateSpeed = rangedDouble("Rotation Speed", "旋转速度", 120.0D, 30.0D, 180.0D, 5.0D);
    private final Setting<Boolean> strictRotation = new Setting<>("Strict Rotation", "严格旋转检测（必须瞄准才攻击）", true);
    private final Setting<Integer> rotationDelay = rangedInt("Rotation Delay", "旋转延迟（tick）", 1, 0, 5, 1);
    private final Setting<Boolean> smoothDisable = new Setting<>("Smooth Disable", "平滑禁用旋转", true);

    // 目标选择
    private final Setting<String> targetMode = new Setting<>("Target Mode", "目标选择模式", "Distance", new String[]{"Distance", "Health", "Angle"});
    private final Setting<Double> fov = rangedDouble("FOV", "视野角度限制", 180.0D, 30.0D, 360.0D, 10.0D);
    private final Setting<Integer> switchDelay = rangedInt("Switch Delay", "切换目标延迟（tick）", 5, 0, 20, 1);

    // 攻击控制
    private final Setting<Boolean> autoBlock = new Setting<>("Auto Block", "自动格挡", false);
    private final Setting<Boolean> raytraceCheck = new Setting<>("Raytrace", "射线检测（只攻击能看到的）", true);
    private final Setting<Boolean> rotationCheck = new Setting<>("Rotation Check", "旋转检测（必须面向目标）", true);

    private List<Entity> targets = new ArrayList<>();
    private long lastAttackTime = 0L;
    private int ticksSinceLastTarget = 0;
    private int rotationTicks = 0;
    private boolean hasRotated = false;
    private int disableRotationTicks = 0;
    private boolean isDisabling = false;

    private final Consumer<PlayerTickEvent> tickListener = this::onPreTick;
    private final Consumer<Render3DEvent> renderListener = this::onRender;

    public KillAura() {
        super("KillAura", "自动攻击附近实体", Category.COMBAT);
        INSTANCE = this;

        addSetting(attackRange);
        addSetting(scanRange);
        addSetting(mode19);
        addSetting(cps);
        addSetting(rotateSpeed);
        addSetting(strictRotation);
        addSetting(rotationDelay);
        addSetting(smoothDisable);
        addSetting(targetMode);
        addSetting(fov);
        addSetting(switchDelay);
        addSetting(autoBlock);
        addSetting(raytraceCheck);
        addSetting(rotationCheck);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetState();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(Render3DEvent.class, renderListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(Render3DEvent.class, renderListener);

        // 平滑清理旋转
        if (smoothDisable.getBoolean() && (target != null || ToolManager.INSTANCE.ROTATION.isServerRotationActive())) {
            isDisabling = true;
            disableRotationTicks = 4;
        } else {
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
        }

        resetState();
    }

    private void resetState() {
        target = null;
        lastTarget = null;
        targets.clear();
        lastAttackTime = 0L;
        ticksSinceLastTarget = 0;
        rotationTicks = 0;
        hasRotated = false;
        disableRotationTicks = 0;
        isDisabling = false;
    }

    public String getSuffix() {
        return targets.isEmpty() ? "Idle" : targets.size() + " Targets";
    }

    private void onPreTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }

        // 处理禁用时的平滑过渡
        if (isDisabling) {
            handleSmoothDisable();
            return;
        }

        // 查找并选择目标
        findTargets();
        selectTarget();

        // 目标切换延迟
        if (target != lastTarget) {
            ticksSinceLastTarget = 0;
            lastTarget = target;
            rotationTicks = 0;
            hasRotated = false;
        }

        if (target != null) {
            ticksSinceLastTarget++;

            // 应用切换延迟
            if (ticksSinceLastTarget < switchDelay.getInt()) {
                return;
            }

            // 执行旋转
            handleRotation();

            // 检查是否可以攻击
            if (canAttack()) {
                attackTarget();
            }
        } else {
            // 失去目标时的平滑处理
            handleTargetLoss();
        }
    }

    private void handleRotation() {
        if (target == null || mc.player == null) {
            return;
        }

        // 计算目标旋转
        Vec3d targetPos = getTargetPoint(target);
        Vec2f targetRotation = ToolManager.INSTANCE.ROTATION.calculate(targetPos);

        // 设置服务器旋转，带射线检测
        ToolManager.INSTANCE.ROTATION.setServerRotation(
            targetRotation,
            rotateSpeed.getDouble(),
            rotation -> {
                if (!raytraceCheck.getBoolean()) {
                    return true;
                }
                HitResult hitResult = ToolManager.INSTANCE.RAY_CAST.rayCast(rotation, attackRange.getDouble(), 0.0F);
                return hitResult instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity().equals(target);
            }
        );

        rotationTicks++;

        // 检查旋转是否完成
        if (rotationTicks >= rotationDelay.getInt()) {
            Vec2f currentRotation = ToolManager.INSTANCE.ROTATION.getServerRotation();
            float yawDiff = Math.abs(ToolManager.INSTANCE.ROTATION.angleDifference(currentRotation.x, targetRotation.x));
            float pitchDiff = Math.abs(currentRotation.y - targetRotation.y);

            hasRotated = yawDiff < 8.0F && pitchDiff < 8.0F;
        }
    }

    private void handleTargetLoss() {
        if (!ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            rotationTicks = 0;
            hasRotated = false;
            return;
        }

        // 平滑过渡到玩家当前朝向
        if (smoothDisable.getBoolean()) {
            Vec2f currentRotation = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            ToolManager.INSTANCE.ROTATION.setServerRotation(currentRotation, 180.0D);

            disableRotationTicks++;
            if (disableRotationTicks >= 3) {
                ToolManager.INSTANCE.ROTATION.clearServerRotation();
                disableRotationTicks = 0;
                rotationTicks = 0;
                hasRotated = false;
            }
        } else {
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
            rotationTicks = 0;
            hasRotated = false;
        }
    }

    private void handleSmoothDisable() {
        if (disableRotationTicks > 0) {
            Vec2f currentRotation = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            ToolManager.INSTANCE.ROTATION.setServerRotation(currentRotation, 180.0D);
            disableRotationTicks--;
        } else {
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
            isDisabling = false;
        }
    }

    private boolean canAttack() {
        if (target == null || mc.player == null) {
            return false;
        }

        // 检查旋转完成度
        if (strictRotation.getBoolean() && !hasRotated) {
            return false;
        }

        // 检查距离
        double distance = mc.player.distanceTo(target);
        if (distance > attackRange.getDouble()) {
            return false;
        }

        // 检查是否面向目标
        if (rotationCheck.getBoolean()) {
            Vec2f targetRotation = ToolManager.INSTANCE.ROTATION.calculate(target);
            Vec2f currentRotation = ToolManager.INSTANCE.ROTATION.getServerRotation();
            float yawDiff = Math.abs(ToolManager.INSTANCE.ROTATION.angleDifference(currentRotation.x, targetRotation.x));

            if (yawDiff > 45.0F) {
                return false;
            }
        }

        // 检查射线
        if (raytraceCheck.getBoolean()) {
            HitResult hitResult = mc.crosshairTarget;
            if (!(hitResult instanceof EntityHitResult entityHitResult)
                || !entityHitResult.getEntity().equals(target)) {
                return false;
            }
        }

        // 检查攻击冷却
        if (mode19.getBoolean()) {
            return mc.player.getAttackCooldownProgress(0.0F) >= 1.0F;
        }

        // 检查CPS限制
        long currentTime = System.currentTimeMillis();
        double baseDelay = 1000.0D / Math.max(1.0D, cps.getDouble());
        long delay = (long) (baseDelay * (0.8D + Math.random() * 0.4D)); // ±20% 随机性

        return currentTime - lastAttackTime >= delay;
    }

    private void attackTarget() {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
    }

    private void findTargets() {
        if (mc.player == null || mc.world == null) {
            targets.clear();
            return;
        }

        double range = scanRange.getDouble();
        double currentFov = fov.getDouble();

        Box searchBox = mc.player.getBoundingBox().expand(range);
        targets = mc.world.getOtherEntities(
            mc.player,
            searchBox,
            entity -> entity instanceof LivingEntity
                && entity != mc.player
                && entity.isAlive()
                && !entity.isSpectator()
                && !entity.isRemoved()
                && !AntiBots.isBot(entity)
                && isTargetEnabled(entity)
                && !Teams.isSameTeam(entity)
                && !FriendManager.isFriend(entity)
                && !isClientUser(entity)
                && mc.player.distanceTo(entity) <= range
                && isInFOV(entity, currentFov)
        );
    }

    private void selectTarget() {
        if (targets.isEmpty()) {
            target = null;
            return;
        }

        String mode = targetMode.getString();
        Entity newTarget = null;

        switch (mode) {
            case "Distance":
                newTarget = targets.stream()
                    .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                    .orElse(null);
                break;

            case "Health":
                newTarget = targets.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .min(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()))
                    .orElse(null);
                break;

            case "Angle":
                newTarget = targets.stream()
                    .min(Comparator.comparingDouble(e -> {
                        Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(e);
                        float yawDiff = Math.abs(ToolManager.INSTANCE.ROTATION.getYawDifference(e.getBoundingBox().getCenter()));
                        return yawDiff;
                    }))
                    .orElse(null);
                break;
        }

        target = newTarget;
    }

    private Vec3d getTargetPoint(Entity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }

        // 瞄准躯干中心点
        double heightOffset = entity.getHeight() * 0.7D;

        // 添加预测（基于目标速度）
        Vec3d velocity = entity.getVelocity();
        double velocityMultiplier = 0.15D; // 轻微预测

        return entity.getPos()
            .add(0, heightOffset, 0)
            .add(velocity.multiply(velocityMultiplier));
    }

    private boolean isInFOV(Entity entity, double fovAngle) {
        if (fovAngle >= 360.0D) {
            return true;
        }

        Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(entity);
        float yawDiff = Math.abs(ToolManager.INSTANCE.ROTATION.angleDifference(mc.player.getYaw(), rotation.x));

        return yawDiff <= fovAngle / 2.0D;
    }

    private boolean isTargetEnabled(Entity entity) {
        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        return targetModule == null || targetModule.isTarget(entity);
    }

    private boolean isClientUser(Entity entity) {
        return mc.player != null && entity instanceof PlayerEntity player
            && player.getUuid().equals(mc.player.getUuid());
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
        RenderSystem.lineWidth(2.0F);

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
            Vector3f relVec = new Vector3f(
                (float) (entityPos.x - camPos.x),
                (float) (entityPos.y - camPos.y),
                (float) (entityPos.z - camPos.z)
            );
            relVec.rotate(invRot);

            matrices.push();
            matrices.translate(relVec.x(), relVec.y(), relVec.z());

            Box box = entity.getBoundingBox().offset(-entityPos.x, -entityPos.y, -entityPos.z);

            // 当前目标用红色，其他用绿色
            Color color = entity.equals(target) ? new Color(255, 50, 50, 150) : new Color(50, 255, 50, 80);

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

    private static Setting<Integer> rangedInt(String name, String description, int defaultValue, int min, int max, int step) {
        Setting<Integer> setting = new Setting<>(name, description, defaultValue);
        setting.setRange((double) min, (double) max, (double) step);
        return setting;
    }
}
