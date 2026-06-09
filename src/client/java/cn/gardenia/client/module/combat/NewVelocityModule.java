package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.movement.StuckInBlockEvent;
import cn.gardenia.client.event.events.movement.NewVelocityGameTickEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.move.BlinkModule;
import cn.gardenia.client.module.move.LongJumpModule;
import cn.gardenia.client.module.move.NoFallModule;
import cn.gardenia.client.module.move.StuckModule;
import cn.gardenia.client.tool.ToolManager;
import cn.gardenia.client.tool.player.GardeniaTickTimer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class NewVelocityModule extends Module {
    public static NewVelocityModule INSTANCE;

    private final Setting<String> mode = new Setting<>("Mode", "Velocity mode", "NoXZ", new String[]{"Jump Reset", "Mix", "NoXZ"});
    private final Setting<Boolean> rotate = new Setting<>("Rotate", "Rotate to knockback direction", false);
    private final Setting<Boolean> tryAttack = new Setting<>("Try Attack", "Attack during Mix release", false);
    private final Setting<Boolean> movementOverride = new Setting<>("Movement Override", "Override movement during Mix", false);
    private final Setting<Boolean> followDirection = new Setting<>("Follow Direction", "Force forward while holding knockback direction", false);
    private final Setting<Integer> rotateTicks = rangedInt("Rotate Ticks", "Ticks to hold velocity rotation", 12, 3, 20, 1);
    private final Setting<Integer> attackAmount = rangedInt("Attack amount", "NoXZ attack count", 5, 1, 20, 1);
    private final Setting<Boolean> instantAttack = new Setting<>("Instant Attack", "Delay NoXZ attacks with timer-style batching", false);
    private final Setting<Boolean> sprintStateCheck = new Setting<>("Sprint state check", "Only attack while sprinting", true);
    private final Setting<Boolean> debug = new Setting<>("Debug", "Print state changes to chat", false);

    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<NewVelocityGameTickEvent> gameTickListener = this::onGameTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<PlayerRespawnEvent> respawnListener = event -> resetAll();
    private final Consumer<StuckInBlockEvent> stuckListener = this::onStuckInBlock;

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> movePacketQueue = new ConcurrentLinkedQueue<>();

    private EntityVelocityUpdateS2CPacket knockbackPacket;
    private Vec2f heldRotation;
    private Vec2f targetRotation;
    private int rotationHeldTicks;
    private boolean suspending;
    private boolean flushing;
    private boolean shouldFlushPacketsOnMotion;

    private Phase jumpResetPhase = Phase.IDLE;
    private int delayTicks;
    private int jumpTicks;

    private boolean mixShouldAttack;
    private boolean mixWasSprinting;
    private int mixWebHitCount;
    private int mixLastPlayerAge;
    private int mixAirTicks;
    private int mixSprintTick = -1;
    private int mixMovementState;
    private boolean mixOverrideForward;
    private boolean mixOverrideBack;
    private boolean mixOverrideLeft;
    private boolean mixOverrideRight;
    private boolean mixOverrideJump;

    private int noxzAttackCooldown;
    private Entity noxzAttackTarget;
    private int noxzAttacksRemaining;
    private int noxzFlagCooldown;
    private boolean noxzShouldJump;
    private int noxzSprintBoostCounter;
    private int noxzHitCounter;
    private int noxzSuspendTicks;
    private float noxzInstantProgress;
    private boolean noxzInstantAttacking;

    private enum Phase {
        IDLE,
        AIR,
        GROUND
    }

    public NewVelocityModule() {
        super("NewVelocity", "OpenZen velocity implementation", Category.COMBAT);
        INSTANCE = this;
        addSetting(mode);
        addSetting(rotate);
        addSetting(tryAttack);
        addSetting(movementOverride);
        addSetting(followDirection);
        addSetting(rotateTicks);
        addSetting(attackAmount);
        addSetting(instantAttack);
        addSetting(sprintStateCheck);
        addSetting(debug);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetAll();
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(NewVelocityGameTickEvent.class, gameTickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
        subscribe(StuckInBlockEvent.class, stuckListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(NewVelocityGameTickEvent.class, gameTickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        unsubscribe(StuckInBlockEvent.class, stuckListener);
        resetAll();
    }

    public String getSuffix() {
        return mode.getString();
    }

    public boolean isActiveMode() {
        return suspending;
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return INSTANCE.onMoveInput(input);
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        if (mc.player == null || isBlockedByOtherModules()) {
            return input;
        }
        if (("Jump Reset".equals(mode.getString()) || "Mix".equals(mode.getString()))
                && followDirection.getBoolean()
                && heldRotation != null) {
            return new PlayerInput(true, false, false, false, input.jump(), input.sneak(), input.sprint());
        }
        if ("Jump Reset".equals(mode.getString()) && jumpTicks > 0) {
            jumpTicks--;
            return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), true, input.sneak(), input.sprint());
        }
        if ("Mix".equals(mode.getString()) && movementOverride.getBoolean() && mixMovementState == 1) {
            return new PlayerInput(mixOverrideForward, mixOverrideBack, mixOverrideLeft, mixOverrideRight,
                    mixOverrideJump || input.jump(), input.sneak(), input.sprint());
        }
        if ("NoXZ".equals(mode.getString())) {
            boolean forward = input.forward() || noxzHitCounter > 0;
            boolean jump = input.jump();
            if (noxzShouldJump) {
                noxzShouldJump = false;
                if (mc.player.isOnGround()
                        && mc.player.isSprinting()
                        && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)
                        && !shouldIgnoreNoXZ()) {
                    jump = true;
                }
            }
            return new PlayerInput(forward, input.backward(), input.left(), input.right(), jump, input.sneak(), input.sprint());
        }
        return input;
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || flushing) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (event.isSend()) {
            if ("NoXZ".equals(mode.getString()) && suspending && packet instanceof PlayerMoveC2SPacket) {
                movePacketQueue.add(packet);
                event.cancel();
            }
            return;
        }
        if (!event.isReceive()) {
            return;
        }

        if (isBlockedByOtherModules()) {
            releaseQueuedPackets();
            resetAll();
            return;
        }

        switch (mode.getString()) {
            case "Jump Reset" -> onJumpResetPacket(event, packet);
            case "Mix" -> onMixPacket(event, packet);
            case "NoXZ" -> onNoXZPacket(event, packet);
            default -> {
            }
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre()) {
            return;
        }
        if ("Mix".equals(mode.getString()) && mc.player != null && (mc.player.isOnGround() || mixAirTicks >= 24)) {
            releaseQueuedPackets();
            suspending = false;
            mixShouldAttack = false;
        }
        if ("NoXZ".equals(mode.getString()) && shouldFlushPacketsOnMotion) {
            releaseQueuedPackets();
            shouldFlushPacketsOnMotion = false;
        }
    }

    private void onGameTick(NewVelocityGameTickEvent event) {
        if (mc.player == null || mc.world == null || isBlockedByOtherModules()) {
            return;
        }
        if ("Jump Reset".equals(mode.getString()) && suspending && jumpResetPhase == Phase.GROUND) {
            return;
        }
        if ("Mix".equals(mode.getString()) && knockbackPacket != null && !movementOverride.getBoolean() && mc.player.hurtTime > 6) {
            mixOverrideJump = true;
            mixMovementState = 1;
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || !event.isPre()) {
            return;
        }
        if (isBlockedByOtherModules()) {
            releaseQueuedPackets();
            resetAll();
            return;
        }

        switch (mode.getString()) {
            case "Jump Reset" -> tickJumpReset();
            case "Mix" -> tickMix();
            case "NoXZ" -> tickNoXZ();
            default -> {
            }
        }

        if (heldRotation != null) {
            rotationHeldTicks++;
            ToolManager.INSTANCE.ROTATION.setServerRotation(heldRotation, 180.0D);
            if (mc.player.hurtTime == 0
                    || rotationHeldTicks > rotateTicks.getInt()
                    || !rotate.getBoolean() && !followDirection.getBoolean()) {
                clearRotation();
            }
        }
    }

    private void onStuckInBlock(StuckInBlockEvent event) {
        if ("Mix".equals(mode.getString()) && event.getState().isOf(Blocks.COBWEB)) {
            mixWebHitCount++;
            if (mc.player != null) {
                mixLastPlayerAge = mc.player.age;
            }
        }
    }

    private void onJumpResetPacket(GlobalPacketEvent event, Packet<?> packet) {
        if (isJumpResetSuspended()) {
            if (suspending) {
                releaseQueuedPackets();
            }
            resetAll();
            return;
        }
        if (suspending && !isJumpResetAllowedPacket(packet)) {
            queueIncoming(event, packet);
            return;
        }
        if (!(packet instanceof EntityVelocityUpdateS2CPacket motion) || motion.getEntityId() != mc.player.getId()) {
            return;
        }

        knockbackPacket = motion;
        Vec2f rotation = velocityRotation(motion);
        if (!mc.player.isOnGround()) {
            if (rotation != null && (rotate.getBoolean() || followDirection.getBoolean())) {
                holdRotation(rotation);
            }
            suspending = true;
            jumpResetPhase = Phase.AIR;
            delayTicks = 20;
        } else {
            targetRotation = rotation;
            suspending = true;
            jumpResetPhase = Phase.GROUND;
            delayTicks = 10;
        }
        queueIncoming(event, packet);
    }

    private void tickJumpReset() {
        if (suspending && jumpResetPhase == Phase.GROUND) {
            return;
        }
        if (suspending && jumpResetPhase == Phase.AIR) {
            if (mc.player.isOnGround()) {
                releaseQueuedPackets();
                resetJumpResetState();
            } else if (delayTicks > 0) {
                delayTicks--;
            } else {
                releaseQueuedPackets();
                resetJumpResetState();
            }
        } else if (suspending && jumpResetPhase == Phase.GROUND) {
            if (delayTicks > 0) {
                delayTicks--;
            } else {
                releaseQueuedPackets();
                if (targetRotation != null && (rotate.getBoolean() || followDirection.getBoolean())) {
                    holdRotation(targetRotation);
                    targetRotation = null;
                }
                resetJumpResetState();
                jumpTicks = 1;
            }
        }
    }

    private void onMixPacket(GlobalPacketEvent event, Packet<?> packet) {
        if (mixWebHitCount > 0 || mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
            releaseQueuedPackets();
            suspending = false;
            knockbackPacket = null;
            clearRotation();
            debug("Mix ignored: player in web or liquid");
            return;
        }
        if (packet instanceof EntityVelocityUpdateS2CPacket motion && motion.getEntityId() == mc.player.getId()) {
            clearRotation();
            knockbackPacket = null;
            if (motion.getVelocityY() > 0) {
                mixSprintTick = 0;
                KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
                if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null && tryAttack.getBoolean()) {
                    mixShouldAttack = true;
                    mixWasSprinting = mc.player.isSprinting();
                } else if (rotate.getBoolean()) {
                    holdRotation(velocityRotation(motion));
                }
                queueIncoming(event, packet);
                suspending = true;
                knockbackPacket = motion;
            }
        }
        if (suspending) {
            if (packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket || isPingLike(packet)) {
                queueIncoming(event, packet);
            }
            if (packet instanceof PlayerPositionLookS2CPacket) {
                debug("Mix flag detected");
                resetAll();
            }
        }
    }

    private void tickMix() {
        if (mixLastPlayerAge < mc.player.age) {
            mixWebHitCount = 0;
        }
        mixAirTicks = mc.player.isOnGround() ? 0 : mixAirTicks + 1;
        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (knockbackPacket != null
                && knockbackPacket.getVelocityY() > 0
                && mc.player.hurtTime > 0
                && killAura != null
                && killAura.isEnabled()
                && killAura.getTarget() != null
                && mixShouldAttack) {
            mixShouldAttack = false;
            for (int i = 0; i < 2; i++) {
                mc.player.setSprinting(false);
                if (mixWasSprinting) {
                    Vec3d motion = mc.player.getVelocity();
                    mc.player.setVelocity(motion.x * 0.6D, motion.y, motion.z * 0.6D);
                }
                attack(killAura.getTarget());
            }
        }

        if (mixSprintTick >= 0) {
            mixSprintTick++;
        }
        if (movementOverride.getBoolean() && knockbackPacket != null) {
            if (mixSprintTick >= 1) {
                if (mixSprintTick <= 2 && mc.player.isOnGround()) {
                    mixOverrideJump = true;
                }
                if (mixSprintTick <= 3) {
                    applyKnockbackDirection();
                    mixMovementState = 1;
                }
            }
            if (mixSprintTick >= 4 && mixSprintTick <= 10) {
                if (mixMovementState == 1) {
                    clearMixMovementOverride();
                    mixMovementState = 0;
                }
            }
            if (mixSprintTick >= 10) {
                mixSprintTick = -1;
            }
        }
    }

    private void onNoXZPacket(GlobalPacketEvent event, Packet<?> packet) {
        if (shouldIgnoreNoXZ()) {
            return;
        }
        if (packet instanceof PlayerPositionLookS2CPacket) {
            if (suspending) {
                releaseNoXZ();
            }
            resetNoXZSuspension();
            debug("NoXZ flag detected");
            noxzFlagCooldown = 2;
        }
        if (noxzFlagCooldown != 0) {
            return;
        }
        if (suspending) {
            if (!isNoXZAllowedPacket(packet)) {
                queueIncoming(event, packet);
            }
            return;
        }
        if (!(packet instanceof EntityVelocityUpdateS2CPacket motion) || motion.getEntityId() != mc.player.getId()) {
            return;
        }
        if (Math.abs(motion.getVelocityX()) > 0.01D || Math.abs(motion.getVelocityZ()) > 0.01D) {
            noxzHitCounter = 1;
        }
        if (motion.getVelocityY() <= 0) {
            return;
        }

        noxzSprintBoostCounter = noxzSprintBoostCounter % 100 + 100;
        if (noxzSprintBoostCounter >= 100) {
            noxzShouldJump = true;
        }
        Entity entity = getAttackTarget();
        boolean validTarget = isValidNoXZTarget(entity) && mc.player.isSprinting();
        if (!mc.player.isOnGround()) {
            suspending = true;
            noxzSuspendTicks = 0;
            knockbackPacket = motion;
            queueIncoming(event, packet);
        } else if (validTarget) {
            noxzAttackTarget = entity;
            noxzAttacksRemaining = attackAmount.getInt();
        } else {
            suspending = true;
            noxzSuspendTicks = 0;
            knockbackPacket = motion;
            queueIncoming(event, packet);
            debug("NoXZ alink wait");
        }
    }

    private void tickNoXZ() {
        if (noxzAttackCooldown > 0) {
            noxzAttackCooldown--;
        }
        if (noxzHitCounter > 0 && ++noxzHitCounter > 2) {
            noxzHitCounter = 0;
        }
        if (shouldIgnoreNoXZ()) {
            clearNoXZTarget();
            if (suspending) {
                releaseNoXZ();
            }
            resetInstantAttack();
            return;
        }
        if (noxzFlagCooldown > 0) {
            noxzFlagCooldown--;
            clearNoXZTarget();
        }
        if (suspending) {
            noxzSuspendTicks++;
            boolean instant = instantAttack.getBoolean();
            if (instant && noxzInstantProgress < 3.0F) {
                GardeniaTickTimer.setTransientMultiplier(0.5F);
                noxzInstantProgress += 0.5F;
                noxzInstantProgress = Math.min(noxzInstantProgress, 3.0F);
            }
            boolean onGround = mc.player.isOnGround();
            boolean timeout = noxzSuspendTicks >= 12;
            if (onGround || timeout) {
                debug(timeout ? "NoXZ alink timeout" : "NoXZ ground");
                if (instant) {
                    GardeniaTickTimer.setTransientMultiplier(1.0F);
                }
                Entity entity = getAttackTarget();
                boolean validTarget = isValidNoXZTarget(entity);
                if (onGround && validTarget && mc.player.isSprinting()) {
                    flushing = true;
                    noxzAttackTarget = entity;
                    noxzAttacksRemaining = attackAmount.getInt();
                    sendQueuedMovePackets();
                    applyKnockbackPacket();
                    if (instant && noxzInstantProgress > 0.0F) {
                        noxzAttacksRemaining = (int) noxzInstantProgress;
                        shouldFlushPacketsOnMotion = true;
                        suspending = false;
                        noxzSuspendTicks = 0;
                        flushing = false;
                        noxzInstantAttacking = true;
                        GardeniaTickTimer.setTransientMultiplier(4.0F);
                    } else {
                        doNoXZAttackSequence();
                        shouldFlushPacketsOnMotion = true;
                        suspending = false;
                        noxzSuspendTicks = 0;
                        flushing = false;
                    }
                } else {
                    releaseNoXZ();
                    if (instant) {
                        noxzInstantProgress = 0.0F;
                    }
                    if (onGround && mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }
                }
                return;
            }
            return;
        }
        if (noxzInstantAttacking) {
            noxzInstantProgress -= 1.0F;
            if (noxzInstantProgress <= 0.0F) {
                resetInstantAttack();
                debug("NoXZ instant attack done");
            } else {
                GardeniaTickTimer.setTransientMultiplier(4.0F);
            }
        }
        if (noxzAttacksRemaining > 0 && noxzAttackTarget != null) {
            doNoXZAttackSequence();
        }
    }

    private void doNoXZAttackSequence() {
        if (noxzAttackTarget == null || !noxzAttackTarget.isAlive()) {
            clearNoXZTarget();
            return;
        }
        if (getBoxDistance(noxzAttackTarget) > 3.7D) {
            clearNoXZTarget();
            return;
        }
        noxzAttackCooldown = 2;
        int attackCount = noxzAttacksRemaining--;
        if (doNoXZAttack(noxzAttackTarget) && instantAttack.getBoolean()) {
            debug("NoXZ attack (" + attackCount + ")");
        }
        if (noxzAttacksRemaining <= 0) {
            clearNoXZTarget();
        }
    }

    private boolean doNoXZAttack(Entity entity) {
        if (mc.player == null || entity == null || mc.interactionManager == null) {
            return false;
        }
        if (sprintStateCheck.getBoolean() && !mc.player.isSprinting()) {
            debug("NoXZ not sprinting");
            return false;
        }
        boolean sprinting = mc.player.isSprinting();
        if (sprinting) {
            mc.player.setSprinting(false);
        }
        attack(entity);
        if (sprinting) {
            Vec3d motion = mc.player.getVelocity();
            mc.player.setVelocity(motion.x * 0.6D, motion.y, motion.z * 0.6D);
        }
        if (!instantAttack.getBoolean()) {
            debug("NoXZ attack (" + noxzAttacksRemaining + ")");
        }
        return true;
    }

    private void attack(Entity entity) {
        if (mc.player == null || mc.interactionManager == null || entity == null) {
            return;
        }
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        KillAura aura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (aura != null) {
            aura.markAttackTimerNow();
        }
    }

    private Entity getAttackTarget() {
        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (killAura != null && killAura.getTarget() != null) {
            return killAura.getTarget();
        }
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            Entity entity = hit.getEntity();
            if (entity instanceof LivingEntity && entity != mc.player && entity.isAlive() && !entity.isSpectator()) {
                return entity;
            }
        }
        return null;
    }

    private boolean isValidNoXZTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !entity.isAlive()) {
            return false;
        }
        if (!living.isAlive() || living.getHealth() <= 0.0F) {
            return false;
        }
        return getBoxDistance(entity) <= 3.7D;
    }

    private boolean shouldIgnoreNoXZ() {
        if (mc.player == null || mc.world == null) {
            return true;
        }
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0.0F) {
            return true;
        }
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            return true;
        }
        if (mc.player.isInLava() || mc.player.isOnFire() || mc.player.isTouchingWater() || mc.player.isClimbing() || mc.player.isSleeping()) {
            return true;
        }
        if (mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB)) {
            return true;
        }
        StuckModule stuck = ModuleManager.INSTANCE.getByClass(StuckModule.class);
        return stuck != null && stuck.isEnabled();
    }

    private boolean isJumpResetSuspended() {
        NoFallModule noFall = ModuleManager.INSTANCE.getByClass(NoFallModule.class);
        NewBackTrackModule backTrack = ModuleManager.INSTANCE.getByClass(NewBackTrackModule.class);
        return noFall != null && noFall.isEnabled()
                || backTrack != null && backTrack.isEnabled() && backTrack.isActive();
    }

    private boolean isBlockedByOtherModules() {
        BlinkModule blink = ModuleManager.INSTANCE.getByClass(BlinkModule.class);
        LongJumpModule longJump = ModuleManager.INSTANCE.getByClass(LongJumpModule.class);
        return blink != null && blink.isEnabled() || longJump != null && longJump.isEnabled();
    }

    private boolean isJumpResetAllowedPacket(Packet<?> packet) {
        return packet instanceof GameMessageS2CPacket;
    }

    private boolean isNoXZAllowedPacket(Packet<?> packet) {
        if (packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof HealthUpdateS2CPacket
                || packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof GameMessageS2CPacket
                || packet instanceof TeamS2CPacket
                || packet instanceof TitleS2CPacket
                || packet instanceof DisconnectS2CPacket
                || packet instanceof PlayerRespawnS2CPacket) {
            return true;
        }
        if (packet instanceof EntityAnimationS2CPacket animation) {
            return animation.getEntityId() != mc.player.getId();
        }
        return packet instanceof EntityStatusS2CPacket;
    }

    private boolean isPingLike(Packet<?> packet) {
        String name = packet.getClass().getSimpleName();
        return name.contains("Ping") || name.contains("KeepAlive");
    }

    private Vec2f velocityRotation(EntityVelocityUpdateS2CPacket packet) {
        if (packet == null || mc.player == null) {
            return null;
        }
        double x = packet.getVelocityX();
        double z = packet.getVelocityZ();
        if (Math.abs(x) < 1.0E-4D && Math.abs(z) < 1.0E-4D) {
            return null;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(x, -z));
        return new Vec2f(yaw, mc.player.getPitch());
    }

    private void holdRotation(Vec2f rotation) {
        if (rotation == null) {
            return;
        }
        heldRotation = rotation;
        rotationHeldTicks = 0;
        ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, 180.0D);
    }

    private void clearRotation() {
        heldRotation = null;
        targetRotation = null;
        rotationHeldTicks = 0;
    }

    private void applyKnockbackDirection() {
        if (mc.player == null || knockbackPacket == null) {
            return;
        }
        float yaw = velocityRotation(knockbackPacket).x;
        float relativeYaw = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        double radians = Math.toRadians(relativeYaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        mixOverrideForward = cos > 0.5D;
        mixOverrideBack = cos < -0.5D;
        mixOverrideRight = sin > 0.5D;
        mixOverrideLeft = sin < -0.5D;
    }

    private void clearMixMovementOverride() {
        mixOverrideForward = false;
        mixOverrideBack = false;
        mixOverrideLeft = false;
        mixOverrideRight = false;
        mixOverrideJump = false;
    }

    private void queueIncoming(GlobalPacketEvent event, Packet<?> packet) {
        packetQueue.add(packet);
        event.cancel();
    }

    private void releaseQueuedPackets() {
        Packet<?> packet;
        while ((packet = packetQueue.poll()) != null) {
            handle(packet);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handle(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        flushing = true;
        try {
            ((Packet) packet).apply((PacketListener) mc.getNetworkHandler());
        } catch (RuntimeException ignored) {
            packetQueue.clear();
        } finally {
            flushing = false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void sendQueuedMovePackets() {
        if (mc.getNetworkHandler() == null) {
            movePacketQueue.clear();
            return;
        }
        Packet<?> packet;
        while ((packet = movePacketQueue.poll()) != null) {
            mc.getNetworkHandler().sendPacket((Packet) packet);
        }
    }

    private void applyKnockbackPacket() {
        if (knockbackPacket != null) {
            handle(knockbackPacket);
            knockbackPacket = null;
        }
    }

    private void releaseNoXZ() {
        flushing = true;
        sendQueuedMovePackets();
        applyKnockbackPacket();
        shouldFlushPacketsOnMotion = true;
        flushing = false;
        suspending = false;
        noxzSuspendTicks = 0;
        resetInstantAttack();
    }

    private void resetAll() {
        releaseQueuedPackets();
        clearRotation();
        resetJumpResetState();
        resetMixState();
        resetNoXZState();
        packetQueue.clear();
        movePacketQueue.clear();
        knockbackPacket = null;
        shouldFlushPacketsOnMotion = false;
        flushing = false;
    }

    private void resetJumpResetState() {
        suspending = false;
        jumpResetPhase = Phase.IDLE;
        delayTicks = 0;
        jumpTicks = 0;
        targetRotation = null;
    }

    private void resetMixState() {
        mixShouldAttack = false;
        mixWasSprinting = false;
        mixWebHitCount = 0;
        mixLastPlayerAge = 0;
        mixAirTicks = 0;
        mixSprintTick = -1;
        mixMovementState = 0;
        clearMixMovementOverride();
    }

    private void resetNoXZState() {
        clearNoXZTarget();
        noxzAttackCooldown = 0;
        noxzFlagCooldown = 0;
        noxzShouldJump = false;
        noxzSprintBoostCounter = 0;
        noxzHitCounter = 0;
        resetNoXZSuspension();
    }

    private void resetNoXZSuspension() {
        suspending = false;
        noxzSuspendTicks = 0;
        knockbackPacket = null;
        packetQueue.clear();
        movePacketQueue.clear();
        noxzInstantProgress = 0.0F;
        noxzInstantAttacking = false;
        GardeniaTickTimer.setTransientMultiplier(1.0F);
    }

    private void clearNoXZTarget() {
        noxzAttackTarget = null;
        noxzAttacksRemaining = 0;
    }

    private void resetInstantAttack() {
        noxzInstantProgress = 0.0F;
        noxzInstantAttacking = false;
        GardeniaTickTimer.setTransientMultiplier(1.0F);
    }

    private double getBoxDistance(Entity entity) {
        if (entity == null || mc.player == null) {
            return Double.MAX_VALUE;
        }
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double x = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double y = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    private void debug(String message) {
        if (debug.getBoolean() && mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("NewVelocity Debug: " + message), false);
        }
    }

    private static Setting<Integer> rangedInt(String name, String description, int defaultValue, int min, int max, int step) {
        Setting<Integer> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
