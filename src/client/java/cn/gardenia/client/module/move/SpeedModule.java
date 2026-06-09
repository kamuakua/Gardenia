package cn.gardenia.client.module.move;

import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

public class SpeedModule extends Module {
    private final Setting<Double> grimHorizontal = rangedDouble("Bounding Box Size", "Expanded player collision scan", 0.4D, 0.0D, 1.0D, 0.1D);
    private final Setting<Double> grimVertical = rangedDouble("In Player Speed", "Speed gain near players", 0.08D, 0.0D, 0.08D, 0.01D);
    private final Setting<Double> grimMoveFlying = rangedDouble("Move Flying Increase", "Base move flying increase", 0.1D, -1.0D, 1.0D, 0.01D);
    private final Setting<Boolean> grimFastFall = new Setting<>("Fast Fall", "Use fast fall packet mode", true);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;

    private int grimAirTicks;
    private int grimGroundState;

    public SpeedModule() {
        super("Speed", "Movement speed adjustments with Grim modes", Category.MOVEMENT);
        addSetting(grimHorizontal);
        addSetting(grimVertical);
        addSetting(grimMoveFlying);
        addSetting(grimFastFall);
    }

    @Override
    public void onEnable() {
        grimAirTicks = 0;
        grimGroundState = 0;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
        grimAirTicks = 0;
        grimGroundState = 0;
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.getPhase() == EventPhase.PRE) {
            grimAirTicks = !mc.player.isOnGround() ? grimAirTicks + 1 : 0;

            if (!isInWeb()
                    && !mc.player.isOnGround()
                    && !(mc.options.forwardKey.isPressed() && (mc.options.rightKey.isPressed() || mc.options.leftKey.isPressed()))
                    && !grimFastFall.getBoolean()) {
                float yaw = (float) (mc.player.getYaw() + 45.0F + Math.random());
                ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(yaw, mc.player.getPitch()), 10.0D);
            }
            return;
        }

        applyStrafeBoost();
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || !grimFastFall.getBoolean()) {
            return;
        }

        if (event.getPhase() == EventPhase.PRE) {
            grimGroundState = grimGroundState == 0 ? (event.isOnGround() ? 1 : 0) : 0;
            event.setOnGround(grimGroundState != 0);
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        } else if (event.getPhase() == EventPhase.POST) {
            if (!mc.player.isOnGround() && grimGroundState >= 0) {
                if (grimAirTicks < 1 || grimAirTicks > 5) {
                    return;
                }
                sendStartFallFlying();
                mc.options.sneakKey.setPressed(false);
                if (grimAirTicks == 2) {
                    mc.player.jump();
                }
            }
        }
    }

    private void applyStrafeBoost() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Box expanded = mc.player.getBoundingBox().expand(grimHorizontal.getDouble());
        for (net.minecraft.entity.Entity entity : mc.world.getOtherEntities(mc.player, expanded, entity -> entity instanceof LivingEntity)) {
            if (entity instanceof LivingEntity) {
                moveFlying(grimVertical.getDouble());
            }
        }

        moveFlying(grimMoveFlying.getDouble() / 1000.0D);
    }

    private void sendStartFallFlying() {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    private void moveFlying(double speed) {
        if (!isMoving()) {
            return;
        }
        double direction = getDirection();
        Vec3d motion = mc.player.getVelocity();
        mc.player.setVelocity(
                motion.x + -Math.sin(direction) * speed,
                motion.y,
                motion.z + Math.cos(direction) * speed
        );
    }

    private double getDirection() {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        if (forward < 0.0F) {
            yaw += 180.0F;
        }
        float factor = 1.0F;
        if (forward < 0.0F) {
            factor = -0.5F;
        } else if (forward > 0.0F) {
            factor = 0.5F;
        }
        if (strafe > 0.0F) {
            yaw -= 90.0F * factor;
        }
        if (strafe < 0.0F) {
            yaw += 90.0F * factor;
        }
        return Math.toRadians(yaw);
    }

    private boolean isMoving() {
        return mc.player != null
                && (Math.abs(mc.player.input.movementForward) > 0.001F
                || Math.abs(mc.player.input.movementSideways) > 0.001F);
    }

    private boolean isInWeb() {
        return mc.world != null
                && mc.player != null
                && mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof CobwebBlock;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
