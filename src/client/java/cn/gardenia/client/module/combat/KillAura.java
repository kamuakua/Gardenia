package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.attack.AttackSlowdownEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KillAura extends Module {
    public static KillAura INSTANCE;

    private final Setting<String> mode = new Setting<>("Mode", "Target selection mode", "Single", new String[]{"Single", "Switch"});
    public final Setting<Double> aimRange = rangedDouble("AimRange", "Rotation target range", 3.0, 0.0, 10.0, 0.1);
    private final Setting<Boolean> throughWalls = new Setting<>("ThroughWalls", "Attack through walls", false);
    public final Setting<Double> wallRange = rangedDouble("WallRange", "Through-wall attack range", 3.0, 0.0, 10.0, 0.1);
    public final Setting<Double> attackRange = rangedDouble("AttackRange", "Attack range", 3.0, 0.0, 10.0, 0.1);
    private final Setting<Double> switchDelay = rangedDouble("SwitchDelay", "Switch delay in ticks", 1.0, 1.0, 10.0, 1.0);
    private final Setting<String> priority = new Setting<>("Priority", "Target sorting", "Distance", new String[]{"Health", "Distance", "Armor", "Baby"});
    private final Setting<Double> minAps = rangedDouble("MinAps", "Minimum attacks per second", 10.0, 1.0, 20.0, 1.0);
    private final Setting<Double> maxAps = rangedDouble("MaxAps", "Maximum attacks per second", 12.0, 1.0, 20.0, 1.0);
    private final Setting<Boolean> autoBlock = new Setting<>("AutoBlock", "Enable block state near targets", false);
    private final Setting<Double> blockRange = rangedDouble("BlockRange", "AutoBlock target range", 3.0, 0.0, 10.0, 0.1);
    private final Setting<String> esp = new Setting<>("ESP", "Target ESP mode", "Off", new String[]{"Off", "Circle", "Around", "Box"});
    private final Setting<String> keepSprintValue = new Setting<>("KeepSprint", "Keep sprint behavior", "Off", new String[]{"Off", "Vanilla", "Prediction"});
    private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", "Disable on respawn", true);

    public Entity target;
    private List<Entity> targets = new ArrayList<>();
    private Entity delayedTarget;
    private int keepSprintTick;
    private boolean pendingStopSprint;
    private boolean blocking;
    private long lastAttackTime;
    private long cpsDelay;
    private int lastCPSMin;
    private int lastCPSMax;
    private int switchIndex;
    private long lastSwitchTime;

    private final Consumer<PlayerTickEvent> preTickListener = this::onPreUpdate;
    private final Consumer<PlayerTickEvent> tickListener = this::onUpdate;
    private final Consumer<AttackSlowdownEvent> slowdownListener = this::onAttackSlowdown;
    private final Consumer<Render3DEvent> renderListener = this::onRender;
    private final Consumer<PlayerRespawnEvent> respawnListener = this::onRespawn;

    public KillAura() {
        super("KillAura", "Automatically attacks entities", Category.COMBAT);
        INSTANCE = this;
        addSetting(mode);
        addSetting(aimRange);
        addSetting(throughWalls);
        addSetting(wallRange);
        addSetting(attackRange);
        addSetting(switchDelay);
        addSetting(priority);
        addSetting(minAps);
        addSetting(maxAps);
        addSetting(autoBlock);
        addSetting(blockRange);
        addSetting(esp);
        addSetting(keepSprintValue);
        addSetting(autoDisable);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetState();
        resetCPS();
        subscribe(PlayerTickEvent.class, preTickListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(AttackSlowdownEvent.class, slowdownListener);
        subscribe(Render3DEvent.class, renderListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, preTickListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(AttackSlowdownEvent.class, slowdownListener);
        unsubscribe(Render3DEvent.class, renderListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        resetState();
        ToolManager.INSTANCE.ROTATION.clearServerRotation();
    }

    public String getSuffix() {
        return targets.size() + " Targets";
    }

    private void onPreUpdate(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        if (keepSprintTick > 0) {
            if (keepSprintTick == 2) {
                keepSprintTick = 1;
            } else if (keepSprintTick == 1) {
                if (delayedTarget != null && filter(delayedTarget)) {
                    executeAttack(delayedTarget);
                }
                pendingStopSprint = false;
                delayedTarget = null;
                keepSprintTick = 0;
            }
        }
    }

    private void onUpdate(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }

        boolean shouldAB = !getTargets(blockRange.getDouble()).isEmpty();
        blocking = autoBlock.getBoolean() && shouldAB;
        targets = getTargets(aimRange.getDouble(), priority.getString());
        if (targets.isEmpty()) {
            target = null;
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
            return;
        }

        int currentMin = (int) Math.round(minAps.getDouble());
        int currentMax = (int) Math.round(maxAps.getDouble());
        if (currentMin != lastCPSMin || currentMax != lastCPSMax) {
            lastCPSMin = currentMin;
            lastCPSMax = currentMax;
            resetCPS();
        }

        if ("Single".equals(mode.getString())) {
            target = targets.get(0);
            rotateTo(target, aimRange.getDouble());
        } else if ("Switch".equals(mode.getString())) {
            updateSwitchTarget();
            if (target != null) {
                rotateTo(target, aimRange.getDouble());
            }
        }

        if (keepSprintTick > 0 || pendingStopSprint) {
            return;
        }

        tryAttack(target, aimRange.getDouble(), wallRange.getDouble());
    }

    private void onAttackSlowdown(AttackSlowdownEvent event) {
        if (mc.player == null || target == null || "Off".equals(keepSprintValue.getString())) {
            return;
        }

        if ("Vanilla".equals(keepSprintValue.getString())) {
            event.setReduce(1.0D);
            event.setSprint(true);
        } else if ("Prediction".equals(keepSprintValue.getString())) {
            event.setSprint(true);
        }
    }

    private void onRender(Render3DEvent event) {
        if ("Off".equals(esp.getString()) || targets == null || targets.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return;
        }

        renderTargetBoxes();
    }

    private void onRespawn(PlayerRespawnEvent event) {
        if (autoDisable.getBoolean()) {
            setEnabled(false);
        }
    }

    public List<Entity> getTargets() {
        return targets;
    }

    public Entity getTarget() {
        return target;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public List<Entity> getTargets(double range) {
        return getTargets(range, "Distance");
    }

    public List<Entity> getTargets(double range, String sortMode) {
        if (mc.player == null || mc.world == null) {
            return new ArrayList<>();
        }

        double rangeSq = range * range;
        Box searchBox = mc.player.getBoundingBox().expand(range);
        Comparator<Entity> comparator = comparator(sortMode);

        return mc.world.getOtherEntities(mc.player, searchBox, this::filter)
                .stream()
                .filter(entity -> getAABBDistance(entity) * getAABBDistance(entity) <= rangeSq)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public boolean filter(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (entity == mc.player || entity.isRemoved() || !living.isAlive() || living.isSpectator()) {
            return false;
        }
        if (Teams.isSameTeam(entity) || FriendManager.isFriend(entity) || AntiBots.isBot(entity)) {
            return false;
        }

        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        return targetModule == null || targetModule.isTarget(entity);
    }

    private Comparator<Entity> comparator(String sortMode) {
        if ("Health".equalsIgnoreCase(sortMode)) {
            return Comparator.comparingDouble(entity -> entity instanceof LivingEntity living ? living.getHealth() : getAABBDistance(entity));
        }
        if ("Armor".equalsIgnoreCase(sortMode)) {
            return Comparator.comparingDouble(entity -> entity instanceof PlayerEntity player ? getPlayerArmorScore(player) : getAABBDistance(entity));
        }
        if ("Baby".equalsIgnoreCase(sortMode)) {
            return Comparator.comparingDouble(entity -> entity instanceof LivingEntity living && living.isBaby() ? 0.0D : getAABBDistance(entity));
        }
        return Comparator.comparingDouble(this::getAABBDistance);
    }

    private float getPlayerArmorScore(PlayerEntity player) {
        double[] score = new double[]{0.0D};
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmorSlot()) {
                continue;
            }

            ItemStack stack = player.getEquippedStack(slot);
            if (stack != null && !stack.isEmpty()) {
                stack.applyAttributeModifiers(slot, (attribute, modifier) -> {
                    if (attribute.matches(EntityAttributes.ARMOR)) {
                        score[0] += modifier.value();
                    }
                });
            }
        }
        return (float) score[0];
    }

    private void updateSwitchTarget() {
        if (targets.isEmpty()) {
            target = null;
            return;
        }

        long now = System.currentTimeMillis();
        long delay = Math.max(1L, Math.round(switchDelay.getDouble())) * 50L;
        if (target == null || now - lastSwitchTime >= delay) {
            switchIndex = (switchIndex + 1) % targets.size();
            lastSwitchTime = now;
        }

        if (switchIndex >= targets.size()) {
            switchIndex = 0;
        }
        target = targets.get(switchIndex);
    }

    private boolean rotateTo(Entity target, double range) {
        Vec2f targetRotation = ToolManager.INSTANCE.RAY_CAST.calculateAdaptive(target, range);
        if (targetRotation == null) {
            targetRotation = ToolManager.INSTANCE.ROTATION.calculate(target);
            ToolManager.INSTANCE.ROTATION.setServerRotation(targetRotation, 180.0D);
            return false;
        }

        ToolManager.INSTANCE.ROTATION.setServerRotation(targetRotation, 180.0D, rotation -> {
            HitResult result = ToolManager.INSTANCE.RAY_CAST.rayCast(rotation, range, 0.0F);
            return result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().equals(target);
        });
        return true;
    }

    private void tryAttack(Entity currentTarget, double range, double wallRange) {
        if (currentTarget == null || targets.isEmpty()) {
            return;
        }

        Vec2f serverRotation = ToolManager.INSTANCE.ROTATION.isServerRotationActive()
                ? ToolManager.INSTANCE.ROTATION.getServerRotation()
                : new Vec2f(mc.player.getYaw(), mc.player.getPitch());

        if (mc.targetedEntity != null && filter(mc.targetedEntity) && getAABBDistance(mc.targetedEntity) <= range) {
            attack(mc.targetedEntity);
        }

        HitResult throughWallResult = throughWalls.getBoolean() ? ToolManager.INSTANCE.RAY_CAST.rayCastEntity(serverRotation, wallRange, 0.0F, true) : null;
        if (isAttackableRayHit(throughWallResult, currentTarget)) {
            attack(((EntityHitResult) throughWallResult).getEntity());
            return;
        }

        HitResult visibleResult = ToolManager.INSTANCE.RAY_CAST.rayCast(serverRotation, range, 0.0F);
        if (isAttackableRayHit(visibleResult, currentTarget)) {
            attack(((EntityHitResult) visibleResult).getEntity());
            return;
        }

        HitResult expandedResult = ToolManager.INSTANCE.RAY_CAST.rayCastEntity(serverRotation, range, 1.0F, false);
        if (isAttackableRayHit(expandedResult, currentTarget)) {
            attack(((EntityHitResult) expandedResult).getEntity());
            return;
        }

        if (mc.targetedEntity == currentTarget && getAABBDistance(currentTarget) <= range) {
            attack(currentTarget);
        }
    }

    private boolean isAttackableRayHit(HitResult result, Entity currentTarget) {
        if (!(result instanceof EntityHitResult entityHitResult)) {
            return false;
        }

        Entity hitEntity = entityHitResult.getEntity();
        return hitEntity == currentTarget || (filter(hitEntity) && "Switch".equals(mode.getString()));
    }

    public void attack(Entity target) {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (System.currentTimeMillis() - lastAttackTime < cpsDelay) {
            return;
        }

        if ("Prediction".equals(keepSprintValue.getString()) && mc.player.isSprinting() && keepSprintTick == 0) {
            pendingStopSprint = true;
            delayedTarget = target;
            keepSprintTick = 2;
            return;
        }

        executeAttack(target);
    }

    private void executeAttack(Entity target) {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (getAABBDistance(target) > attackRange.getDouble()) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        resetCPS();
    }

    public void resetCPS() {
        double min = Math.min(minAps.getDouble(), maxAps.getDouble());
        double max = Math.max(minAps.getDouble(), maxAps.getDouble());
        double aps = min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);
        cpsDelay = (long) (1000.0D / Math.max(1.0D, aps));
        lastAttackTime = System.currentTimeMillis();
    }

    private double getAABBDistance(Entity entity) {
        if (mc.player == null || entity == null) {
            return Double.MAX_VALUE;
        }

        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double x = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double y = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
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
            int alpha = "Box".equals(esp.getString()) ? 60 : "Around".equals(esp.getString()) ? 45 : 35;
            boolean selected = entity.equals(target);
            float r = (selected ? 200.0F : 0.0F) / 255.0F;
            float g = (selected ? 0.0F : 200.0F) / 255.0F;
            float b = 0.0F;
            float a = alpha / 255.0F;
            VertexRendering.drawBox(matrices, lines, box, r, g, b, a);

            matrices.pop();
        }

        providers.draw(RenderLayer.LINES);
        mvStack.popMatrix();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void resetState() {
        if (targets != null) {
            targets.clear();
        }
        target = null;
        delayedTarget = null;
        keepSprintTick = 0;
        pendingStopSprint = false;
        blocking = false;
        switchIndex = 0;
        lastSwitchTime = 0L;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
