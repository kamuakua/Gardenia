package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.attack.AttackSlowdownEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.event.events.render.Render3DEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.misc.ClientFriendModule;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.module.move.BlinkModule;
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
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    private final Setting<String> mode = new Setting<>("Mode", "Target switch mode", "Single", new String[]{"Single", "Switch"});
    public final Setting<Double> aimRange = rangedDouble("AimRange", "Target aim range", 3.0D, 0.0D, 10.0D, 0.1D);
    private final Setting<Boolean> throughWalls = new Setting<>("ThroughWalls", "Attack through walls", false);
    public final Setting<Double> wallRange = rangedDouble("WallRange", "Through-wall range", 3.0D, 0.0D, 10.0D, 0.1D);
    public final Setting<Double> attackRange = rangedDouble("AttackRange", "Attack execution range", 3.0D, 0.0D, 10.0D, 0.1D);
    private final Setting<Double> switchDelay = rangedDouble("SwitchDelay", "Switch delay in ticks", 1.0D, 1.0D, 10.0D, 1.0D);
    private final Setting<String> priority = new Setting<>("Priority", "Target priority", "Distance", new String[]{"Health", "Distance", "Armor", "Baby"});
    private final Setting<Double> minAps = rangedDouble("MinAps", "Minimum attacks per second", 10.0D, 1.0D, 20.0D, 1.0D);
    private final Setting<Double> maxAps = rangedDouble("MaxAps", "Maximum attacks per second", 12.0D, 1.0D, 20.0D, 1.0D);
    private final Setting<Boolean> autoBlock = new Setting<>("AutoBlock", "Track block target range", false);
    private final Setting<Double> blockRange = rangedDouble("BlockRange", "AutoBlock target range", 3.0D, 0.0D, 10.0D, 0.1D);
    private final Setting<String> esp = new Setting<>("ESP", "ESP mode", "Off", new String[]{"Off", "Circle", "Around", "Box"});
    private final Setting<String> keepSprintValue = new Setting<>("KeepSprint", "Keep sprint mode", "Off", new String[]{"Off", "Vanilla", "Prediction"});
    private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", "Disable on respawn", true);

    private final Consumer<PlayerTickEvent> tickListener = this::onPlayerTick;
    private final Consumer<AttackSlowdownEvent> attackSlowdownListener = this::onAttackSlowdown;
    private final Consumer<PlayerRespawnEvent> respawnListener = this::onRespawn;
    private final Consumer<Render3DEvent> renderListener = this::onRender;

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
    private boolean wasOnGroundBeforeAttack;

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
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(AttackSlowdownEvent.class, attackSlowdownListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
        subscribe(Render3DEvent.class, renderListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(AttackSlowdownEvent.class, attackSlowdownListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        unsubscribe(Render3DEvent.class, renderListener);
        ToolManager.INSTANCE.ROTATION.clearServerRotation();
        resetState();
    }

    public String getSuffix() {
        return targets.size() + " Targets";
    }

    private void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }

        onPreUpdate();
        onUpdate();
    }

    private void onPreUpdate() {
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

    private void onUpdate() {
        boolean blinkEnabled = isModuleEnabled(BlinkModule.class);
        boolean shouldAB = !blinkEnabled && !getTargets(blockRange.getDouble()).isEmpty();
        if (blinkEnabled) {
            return;
        }

        blocking = shouldAB;
        targets = getTargets(aimRange.getDouble(), priority.getString());
        if (targets.isEmpty()) {
            target = null;
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

    private void onRespawn(PlayerRespawnEvent event) {
        if (autoDisable.getBoolean()) {
            setEnabled(false);
        }
    }

    private void onRender(Render3DEvent event) {
        if ("Off".equals(esp.getString()) || targets == null || targets.isEmpty()) {
            return;
        }
        if (mc.player == null || mc.world == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return;
        }

        renderTargetBoxes();
    }

    public List<Entity> getTargets() {
        return targets;
    }

    public Entity getTarget() {
        return target;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean wasOnGroundBeforeAttack() {
        return wasOnGroundBeforeAttack;
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
                .filter(entity -> {
                    double distance = getAABBDistance(entity);
                    return distance * distance <= rangeSq;
                })
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
        if (Teams.isSameTeam(entity) || FriendManager.isFriend(entity) || ClientFriendModule.isUser(entity)) {
            return false;
        }
        if (AntiBots.isBot(entity)) {
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
        float score = 0.0F;
        for (ItemStack stack : player.getInventory().armor) {
            if (stack != null && stack.getItem() instanceof ArmorItem) {
                score += armorDefense(stack);
            }
        }
        return score;
    }

    private float armorDefense(ItemStack stack) {
        if (stack.isOf(Items.LEATHER_HELMET) || stack.isOf(Items.LEATHER_BOOTS)
                || stack.isOf(Items.GOLDEN_HELMET) || stack.isOf(Items.GOLDEN_BOOTS)
                || stack.isOf(Items.CHAINMAIL_BOOTS)) {
            return 1.0F;
        }
        if (stack.isOf(Items.LEATHER_LEGGINGS) || stack.isOf(Items.GOLDEN_LEGGINGS)
                || stack.isOf(Items.CHAINMAIL_HELMET) || stack.isOf(Items.IRON_HELMET)
                || stack.isOf(Items.IRON_BOOTS) || stack.isOf(Items.DIAMOND_HELMET)
                || stack.isOf(Items.DIAMOND_BOOTS) || stack.isOf(Items.NETHERITE_HELMET)
                || stack.isOf(Items.NETHERITE_BOOTS) || stack.isOf(Items.TURTLE_HELMET)) {
            return 2.0F;
        }
        if (stack.isOf(Items.LEATHER_CHESTPLATE) || stack.isOf(Items.GOLDEN_CHESTPLATE)
                || stack.isOf(Items.CHAINMAIL_LEGGINGS)) {
            return 3.0F;
        }
        if (stack.isOf(Items.CHAINMAIL_CHESTPLATE) || stack.isOf(Items.IRON_LEGGINGS)) {
            return 5.0F;
        }
        if (stack.isOf(Items.IRON_CHESTPLATE) || stack.isOf(Items.DIAMOND_LEGGINGS)
                || stack.isOf(Items.NETHERITE_LEGGINGS)) {
            return 6.0F;
        }
        if (stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.NETHERITE_CHESTPLATE)) {
            return 8.0F;
        }
        return 0.0F;
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

        if (mc.crosshairTarget instanceof EntityHitResult hit
                && filter(hit.getEntity())
                && getAABBDistance(hit.getEntity()) <= range) {
            attack(hit.getEntity());
        }

        HitResult throughWallResult = ToolManager.INSTANCE.RAY_CAST.rayCastEntity(serverRotation, wallRange, 0.0F, true);
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

        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() == currentTarget
                && getAABBDistance(hit.getEntity()) <= range) {
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

        wasOnGroundBeforeAttack = mc.player.isOnGround();
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

    public void markAttackTimerNow() {
        lastAttackTime = System.currentTimeMillis();
        if (mc.player != null) {
            wasOnGroundBeforeAttack = mc.player.isOnGround();
        }
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
        RenderSystem.lineWidth("Box".equals(esp.getString()) ? 2.0F : 1.0F);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();

        Quaternionf cameraRotation = camera.getRotation();
        Quaternionf inverseRotation = new Quaternionf(cameraRotation).conjugate();
        modelView.rotate(cameraRotation);

        VertexConsumerProvider.Immediate providers = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = providers.getBuffer(RenderLayer.LINES);
        MatrixStack matrices = new MatrixStack();

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            Vec3d entityPos = entity.getPos();
            Vector3f relative = new Vector3f(
                    (float) (entityPos.x - camPos.x),
                    (float) (entityPos.y - camPos.y),
                    (float) (entityPos.z - camPos.z)
            );
            relative.rotate(inverseRotation);

            matrices.push();
            matrices.translate(relative.x(), relative.y(), relative.z());

            Box box = entity.getBoundingBox().offset(-entityPos.x, -entityPos.y, -entityPos.z);
            int alpha = "Box".equals(esp.getString()) ? 60 : "Around".equals(esp.getString()) ? 45 : 35;
            float red = entity.equals(target) ? 200.0F / 255.0F : 0.0F;
            float green = entity.equals(target) ? 0.0F : 200.0F / 255.0F;
            float blue = 0.0F;
            float opacity = alpha / 255.0F;
            VertexRendering.drawBox(matrices, lines, box, red, green, blue, opacity);

            matrices.pop();
        }

        providers.draw(RenderLayer.LINES);
        modelView.popMatrix();
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
        wasOnGroundBeforeAttack = false;
    }

    private <T extends Module> boolean isModuleEnabled(Class<T> moduleClass) {
        T module = ModuleManager.INSTANCE.getByClass(moduleClass);
        return module != null && module.isEnabled();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
