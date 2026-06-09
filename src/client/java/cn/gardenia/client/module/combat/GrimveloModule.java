package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.move.LongJumpModule;
import cn.gardenia.client.module.move.StuckModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GrimveloModule extends Module {
    public static GrimveloModule INSTANCE;

    private static final int MAX_DELAY_TICKS = 40;
    private static final long MAX_DELAY_MS = 2000L;

    private final Setting<Boolean> delayUntilGround = new Setting<>("Delay Till Ground", "Release delayed velocity only after touching ground", false);
    private final Setting<Double> targetMotion = rangedDouble("Target Motion", "Minimum horizontal motion to delay", 0.15D, 0.05D, 0.5D, 0.05D);
    private final Setting<Boolean> multiTarget = new Setting<>("Multi Target", "Search nearby players when KillAura target is not releasable", true);
    private final Setting<Double> releaseDistance = rangedDouble("Release Distance", "Distance required to release delayed velocity", 1.6D, 0.1D, 3.0D, 0.1D);
    private final Setting<Boolean> fireballCooldown = new Setting<>("Fireball Cooldown", "Avoid delaying immediately after fire charge use", true);
    private final Setting<Boolean> modernCombatMode = new Setting<>("1.9+", "Wait for modern attack cooldown while reducing", false);
    private final Setting<Boolean> debug = new Setting<>("Debug", "Print state changes to chat", false);

    private final Queue<Packet<?>> blockedPackets = new ConcurrentLinkedQueue<>();
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private EntityVelocityUpdateS2CPacket capturedVelocityPacket;
    private Vec3d capturedVelocity = Vec3d.ZERO;
    private Entity reduceTarget;
    private int resetCooldownTicks;
    private int delayTicks;
    private long blockStartedAt;
    private boolean replaying;

    private static boolean delayingVelocity;
    private static int queuedAttackTicks;

    public GrimveloModule() {
        super("Grimvelo", "Velocity delay and reduction", Category.COMBAT);
        INSTANCE = this;
        addSetting(delayUntilGround);
        addSetting(targetMotion);
        addSetting(multiTarget);
        addSetting(releaseDistance);
        addSetting(fireballCooldown);
        addSetting(modernCombatMode);
        addSetting(debug);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetState(false);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        resetState(true);
    }

    public boolean isDelayingVelocity() {
        return delayingVelocity;
    }

    public int getQueuedAttackTicks() {
        return queuedAttackTicks;
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return INSTANCE.onMoveInput(input);
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        if (mc.player == null || resetCooldownTicks > 0) {
            return input;
        }
        if (queuedAttackTicks > 0 && !input.forward() && !mc.player.isUsingItem()) {
            return new PlayerInput(true, input.backward(), input.left(), input.right(), input.jump(), input.sneak(), input.sprint());
        }
        return input;
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || replaying) {
            resetState(false);
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.isSend()) {
            handleFireballCooldown(packet);
            return;
        }
        if (!event.isReceive()) {
            return;
        }

        if (packet instanceof PlayerPositionLookS2CPacket && delayingVelocity) {
            resetCooldownTicks = 2;
            resetState(true);
            return;
        }

        if (delayingVelocity && shouldBlockPacket(packet)) {
            blockedPackets.offer(packet);
            event.cancel();
            return;
        }

        if (!(packet instanceof EntityVelocityUpdateS2CPacket motion) || motion.getEntityId() != mc.player.getId()) {
            return;
        }
        if (resetCooldownTicks > 0 || isInvalid() || !shouldDelayVelocity(motion)) {
            return;
        }

        capturedVelocityPacket = motion;
        capturedVelocity = new Vec3d(motion.getVelocityX(), motion.getVelocityY(), motion.getVelocityZ());
        reduceTarget = getReleaseTarget(false);
        delayingVelocity = true;
        delayTicks = 0;
        blockStartedAt = System.currentTimeMillis();
        event.cancel();
        debug("Delay velocity: " + format(horizontalLength(capturedVelocity)));
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre()) {
            return;
        }
        if (mc.player == null || mc.world == null || isInvalid()) {
            resetState(true);
            return;
        }

        if (resetCooldownTicks > 0) {
            resetCooldownTicks--;
            return;
        }

        if (delayingVelocity) {
            Entity target = getReleaseTarget(false);
            if (target != null) {
                releaseForReduce(target);
            } else {
                tickDelay();
            }
        }

        tickQueuedAttacks();
    }

    private void tickDelay() {
        if (!delayingVelocity) {
            return;
        }

        delayTicks++;
        long elapsed = System.currentTimeMillis() - blockStartedAt;
        if (delayTicks >= MAX_DELAY_TICKS || elapsed >= MAX_DELAY_MS) {
            resetState(true);
            return;
        }

        Entity target = getReleaseTarget(true);
        if (target == null) {
            resetState(true);
            return;
        }

        if (canRelease(target, false)) {
            releaseForReduce(target);
        }
    }

    private void tickQueuedAttacks() {
        if (queuedAttackTicks <= 0) {
            return;
        }

        queuedAttackTicks--;
        if (modernCombatMode.getBoolean()) {
            performModernReduce();
        } else {
            performReduceAttack();
        }
    }

    private boolean shouldDelayVelocity(EntityVelocityUpdateS2CPacket motion) {
        KillAura aura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (aura == null || !aura.isEnabled()) {
            return false;
        }
        return Math.hypot(motion.getVelocityX(), motion.getVelocityZ()) >= targetMotion.getDouble();
    }

    private boolean canRelease(Entity target, boolean requireDistance) {
        if (mc.player.isUsingItem() || !mc.player.isSprinting()) {
            return false;
        }
        if (delayUntilGround.getBoolean() && !mc.player.isOnGround()) {
            return false;
        }
        return !requireDistance || getBoxDistance(target) <= releaseDistance.getDouble();
    }

    private int calculateAttackTicks(double motion) {
        if (motion < targetMotion.getDouble()) {
            return -1;
        }

        for (int i = 0; i < 8; i++) {
            motion *= 0.6D;
            if (motion < targetMotion.getDouble()) {
                return i;
            }
        }
        return 8;
    }

    private void releaseForReduce(Entity target) {
        Vec3d motion = mc.player.getVelocity();
        int attacks = calculateAttackTicks(Math.hypot(motion.x, motion.z)) + 1;
        reduceTarget = target;
        delayingVelocity = false;
        capturedVelocityPacket = null;
        capturedVelocity = Vec3d.ZERO;
        releaseBlockedPackets();
        queuedAttackTicks = Math.max(0, attacks);
    }

    private void performReduceAttack() {
        Entity target = getReduceTarget();
        if (target == null || !mc.player.isSprinting()) {
            reduceTarget = null;
            return;
        }

        attack(target);
        scaleHorizontalMotion(0.6D);
        reduceTarget = null;
    }

    private void performModernReduce() {
        Entity target = getReduceTarget();
        if (target == null) {
            reduceTarget = null;
            return;
        }
        if (mc.player.getAttackCooldownProgress(0.0F) < 1.0F) {
            queuedAttackTicks = Math.max(queuedAttackTicks, 1);
            return;
        }

        attack(target);
        scaleHorizontalMotion(0.6D);
        reduceTarget = null;
    }

    private Entity getReduceTarget() {
        if (isValidTarget(reduceTarget)) {
            return reduceTarget;
        }
        reduceTarget = getReleaseTarget(false);
        return reduceTarget;
    }

    private Entity getReleaseTarget(boolean allowMultiTargetSearch) {
        KillAura aura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (aura == null || !aura.isEnabled() || !isValidTarget(aura.getTarget())) {
            return null;
        }

        if (canRelease(aura.getTarget(), true)) {
            return aura.getTarget();
        }

        if (!allowMultiTargetSearch || !multiTarget.getBoolean()) {
            return null;
        }

        for (Entity entity : aura.getTargets()) {
            if (isSelectableTarget(entity)) {
                return entity;
            }
        }

        Box box = mc.player.getBoundingBox().expand(Math.max(3.0D, releaseDistance.getDouble()));
        for (PlayerEntity player : mc.world.getEntitiesByClass(PlayerEntity.class, box, this::isSelectableTarget)) {
            return player;
        }
        return null;
    }

    private boolean isSelectableTarget(Entity entity) {
        return entity instanceof PlayerEntity && isValidTarget(entity) && getBoxDistance(entity) <= releaseDistance.getDouble();
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player) {
            return false;
        }
        if (!living.isAlive() || living.isSpectator()) {
            return false;
        }
        return !(living instanceof PlayerEntity player) || !AntiBots.isBot(player);
    }

    private void attack(Entity entity) {
        if (mc.getNetworkHandler() == null || mc.player == null || entity == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.player.swingHand(Hand.MAIN_HAND);
        resetKillAuraTimer();
    }

    private void scaleHorizontalMotion(double multiplier) {
        Vec3d motion = mc.player.getVelocity();
        mc.player.setVelocity(motion.x * multiplier, motion.y, motion.z * multiplier);
    }

    private void handleFireballCooldown(Packet<?> packet) {
        if (!fireballCooldown.getBoolean() || !(packet instanceof PlayerInteractItemC2SPacket useItem) || mc.player == null) {
            return;
        }

        ItemStack stack = mc.player.getStackInHand(useItem.getHand());
        if (!stack.isEmpty() && stack.isOf(Items.FIRE_CHARGE)) {
            resetCooldownTicks = 10;
        }
    }

    private boolean shouldBlockPacket(Packet<?> packet) {
        return !(packet instanceof PlayerPositionLookS2CPacket)
                && !(packet instanceof DisconnectS2CPacket)
                && !(packet instanceof PlayerRespawnS2CPacket);
    }

    private void releaseBlockedPackets() {
        Packet<?> packet;
        while ((packet = blockedPackets.poll()) != null) {
            handle(packet);
        }
    }

    private void resetState(boolean releasePackets) {
        if (releasePackets) {
            releaseBlockedPackets();
        } else {
            blockedPackets.clear();
        }
        delayingVelocity = false;
        queuedAttackTicks = 0;
        capturedVelocityPacket = null;
        capturedVelocity = Vec3d.ZERO;
        reduceTarget = null;
        delayTicks = 0;
        blockStartedAt = 0L;
    }

    private boolean isInvalid() {
        LongJumpModule longJump = ModuleManager.INSTANCE.getByClass(LongJumpModule.class);
        StuckModule stuck = ModuleManager.INSTANCE.getByClass(StuckModule.class);
        return longJump != null && longJump.isEnabled() || stuck != null && stuck.isEnabled();
    }

    private double getBoxDistance(Entity entity) {
        Vec3d origin = mc.player.getPos();
        Box box = entity.getBoundingBox();
        double x = MathHelper.clamp(origin.x, box.minX, box.maxX);
        double y = MathHelper.clamp(origin.y, box.minY, box.maxY);
        double z = MathHelper.clamp(origin.z, box.minZ, box.maxZ);
        return origin.distanceTo(new Vec3d(x, y, z));
    }

    private double horizontalLength(Vec3d velocity) {
        return Math.hypot(velocity.x, velocity.z);
    }

    private String format(double value) {
        return String.format("%.3f", value);
    }

    private void debug(String message) {
        if (debug.getBoolean() && mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("Grimvelo: " + message), false);
        }
    }

    private void resetKillAuraTimer() {
        KillAura aura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (aura != null) {
            aura.markAttackTimerNow();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handle(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        replaying = true;
        try {
            ((Packet) packet).apply((PacketListener) mc.getNetworkHandler());
        } finally {
            replaying = false;
        }
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
