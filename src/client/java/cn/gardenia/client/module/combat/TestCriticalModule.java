package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.attack.AttackEntityEvent;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;

import java.util.function.Consumer;

public class TestCriticalModule extends Module {
    public static TestCriticalModule INSTANCE;

    private static final float RANGE = 3.0F;
    private static final int LEGACY_DELAY_TICKS = 1;

    private final Consumer<AttackEntityEvent> attackListener = this::onAttack;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;

    private boolean restoreSprint;
    private int suppressSprintTicks;
    private int attackTicks;
    private int critAttacks;
    private int totalAttacks;
    private String suffix = "Idle";

    public TestCriticalModule() {
        super("TestCritical", "Modern critical sprint reset", Category.COMBAT);
        INSTANCE = this;
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || INSTANCE.mc.player == null || INSTANCE.mc.world == null) {
            return input;
        }

        Entity target = INSTANCE.getTarget();
        if (!INSTANCE.isTargetValid(target) || !INSTANCE.canCritEnvironment()) {
            return input;
        }

        boolean jump = input.jump();
        if (INSTANCE.mc.player.isOnGround()
                && INSTANCE.hasJumpRoom()
                && !INSTANCE.mc.player.isUsingItem()
                && !INSTANCE.mc.player.isSneaking()) {
            jump = true;
        }

        boolean sprint = input.sprint();
        if (INSTANCE.shouldStopSprint()) {
            INSTANCE.restoreSprint = INSTANCE.restoreSprint || INSTANCE.mc.options.sprintKey.isPressed();
            INSTANCE.suppressSprintTicks = Math.max(INSTANCE.suppressSprintTicks, 2);
            INSTANCE.mc.options.sprintKey.setPressed(false);
            INSTANCE.mc.player.setSprinting(false);
            sprint = false;
        }

        return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jump, input.sneak(), sprint);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        restoreSprint = false;
        suppressSprintTicks = 0;
        attackTicks = LEGACY_DELAY_TICKS;
        critAttacks = 0;
        totalAttacks = 0;
        suffix = "Idle";
        subscribe(AttackEntityEvent.class, attackListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(AttackEntityEvent.class, attackListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        restoreSprint = false;
        suppressSprintTicks = 0;
        attackTicks = 0;
        critAttacks = 0;
        totalAttacks = 0;
        suffix = "Idle";
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean prepareForAuraAttack(Entity target) {
        return prepareAttack(target);
    }

    public void restoreAfterAuraAttack() {
        restoreSprintIfNeeded();
    }

    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.isPre()) {
            prepareAttack(event.getTarget());
        } else if (event.isPost()) {
            restoreSprintIfNeeded();
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.options == null) {
            return;
        }

        if (event.isPost()) {
            attackTicks++;
            if (suppressSprintTicks > 0) {
                suppressSprintTicks--;
            }
            if (suppressSprintTicks == 0 && restoreSprint) {
                mc.options.sprintKey.setPressed(true);
                restoreSprint = false;
            }
            return;
        }

        if (event.isPre() && suppressSprintTicks > 0) {
            mc.options.sprintKey.setPressed(false);
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        Entity target = getTarget();
        if (!isTargetValid(target) || !canCritEnvironment()) {
            suffix = "Idle";
            return;
        }

        String state = isCriticalWindow() && isSprintOff() ? "Crit" : mc.player.isOnGround() ? "Jump" : "Air";
        suffix = totalAttacks > 0
                ? String.format("%s %.0f%%", state, critAttacks * 100.0D / totalAttacks)
                : state;
    }

    private boolean prepareAttack(Entity target) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null
                || !isTargetValid(target) || !canCritEnvironment() || attackTicks < LEGACY_DELAY_TICKS) {
            recordAttack(false);
            return false;
        }

        if (!isSprintOff()) {
            stopSprinting();
        }

        boolean critical = isCriticalWindow() && isSprintOff();
        recordAttack(critical);
        return critical;
    }

    private void restoreSprintIfNeeded() {
        if (restoreSprint && mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            suppressSprintTicks = Math.max(suppressSprintTicks, 1);
        }
    }

    private boolean shouldStopSprint() {
        return mc.player != null
                && !isSprintOff()
                && !mc.player.isOnGround()
                && (isCriticalWindow()
                || mc.player.getVelocity().y > 0.0D && (mc.player.getVelocity().y - 0.08D) * 0.98D < 0.0D);
    }

    private void stopSprinting() {
        restoreSprint = true;
        suppressSprintTicks = Math.max(suppressSprintTicks, 2);
        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
        mc.player.setSprinting(false);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private boolean isSprintOff() {
        return mc.player != null && !mc.player.isSprinting() && (mc.options == null || !mc.options.sprintKey.isPressed());
    }

    private boolean isCriticalWindow() {
        return mc.player != null
                && mc.player.fallDistance > 0.0F
                && mc.player.getVelocity().y < 0.0D
                && !mc.player.isOnGround();
    }

    private boolean canCritEnvironment() {
        return mc.player != null
                && !mc.player.hasVehicle()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.isSleeping()
                && !mc.player.isGliding()
                && !mc.player.getAbilities().flying
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                && !mc.player.hasNoGravity();
    }

    private boolean isTargetValid(Entity target) {
        return target instanceof LivingEntity living
                && target != mc.player
                && living.isAlive()
                && living.getHealth() > 0.0F
                && !target.isSpectator()
                && mc.player != null
                && mc.player.distanceTo(target) <= RANGE;
    }

    private boolean hasJumpRoom() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        Box jumpBox = mc.player.getBoundingBox().offset(0.0D, 0.42D, 0.0D);
        return !mc.world.getBlockCollisions(mc.player, jumpBox).iterator().hasNext();
    }

    private Entity getTarget() {
        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            return killAura.getTarget();
        }
        if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
            return hitResult.getEntity();
        }
        return null;
    }

    private void recordAttack(boolean critical) {
        totalAttacks++;
        attackTicks = 0;
        if (critical) {
            critAttacks++;
        }
    }
}
