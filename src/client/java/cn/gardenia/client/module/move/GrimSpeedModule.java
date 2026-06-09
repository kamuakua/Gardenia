package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.combat.KillAura;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class GrimSpeedModule extends Module {
    public static GrimSpeedModule INSTANCE;

    private final Setting<Boolean> logging = new Setting<>("Logging", "Print debug state to chat", false);
    private final Setting<Boolean> grimFastFall = new Setting<>("Fast Fall", "Use Grim fast fall packet mode", true);
    private final Setting<Boolean> tick1 = new Setting<>("Fast Fall On 1 Tick", "Trigger fast fall on first configured air tick", false);
    private final Setting<Boolean> tick2 = new Setting<>("Fast Fall On 2 Tick", "Trigger fast fall on second configured air tick", false);
    private final Setting<Boolean> tick3 = new Setting<>("Fast Fall On 3 Tick", "Trigger fast fall on third configured air tick", false);
    private final Setting<Boolean> tick4 = new Setting<>("Fast Fall On 4 Tick", "Trigger fast fall on fourth configured air tick", false);
    private final Setting<Boolean> tick5 = new Setting<>("Fast Fall On 5 Tick", "Reserved fifth air tick toggle", false);
    private final Setting<Boolean> rotation = new Setting<>("Rotation", "Apply airborne server rotation", true);
    private final Setting<Double> grimMoveFlying = rangedDouble("Speed", "Horizontal boost during fast fall", 0.1D, 0.0D, 10.0D, 0.01D);
    private final Setting<Double> startFall = rangedDouble("Start Fast Fall", "Air ticks before fast fall checks", 2.0D, 0.0D, 10.0D, 1.0D);
    private final Setting<Double> packetCount = rangedDouble("Packet Count", "START_FALL_FLYING packet count", 1.0D, 1.0D, 10.0D, 1.0D);
    private final Setting<Double> skipTicks = rangedDouble("Skip Ticks", "Skipped tick budget mirror", 2.0D, 1.0D, 10.0D, 1.0D);
    private final Setting<Double> ticks = rangedDouble("Ticks", "Extra player ticks to run during fast fall", 3.0D, 1.0D, 10.0D, 1.0D);
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private int airTicks;
    private double pendingSkipTicks;

    public GrimSpeedModule() {
        super("GrimSpeed", "Movement speed adjustments with Grim modes", Category.MOVEMENT);
        INSTANCE = this;
        addSetting(logging);
        addSetting(grimFastFall);
        addSetting(tick1);
        addSetting(tick2);
        addSetting(tick3);
        addSetting(tick4);
        addSetting(tick5);
        addSetting(rotation);
        addSetting(grimMoveFlying);
        addSetting(startFall);
        addSetting(packetCount);
        addSetting(skipTicks);
        addSetting(ticks);
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || INSTANCE.mc.player == null || INSTANCE.mc.world == null) {
            return input;
        }
        return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), true, input.sneak(), input.sprint());
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        airTicks = 0;
        pendingSkipTicks = 0.0D;
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        airTicks = 0;
        pendingSkipTicks = 0.0D;
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || !grimFastFall.getBoolean()) {
            return;
        }

        if (event.isPre()) {
            airTicks = mc.player.isOnGround() ? 0 : airTicks + 1;
            return;
        }

        if (event.isPost() && !mc.player.isOnGround() && airTicks > startFall.getInt()) {
            int fastFallTick = airTicks - startFall.getInt();
            log("Air ticks: " + airTicks);
            if (shouldFastFallOnTick(fastFallTick)) {
                sendFastFallPackets();
            }
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre() || !rotation.getBoolean()) {
            return;
        }

        if (!mc.player.isOnGround() && !isModuleEnabled("Scaffold") && !hasKillAuraTarget()) {
            float random = (float) ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D);
            float yaw = MathHelper.wrapDegrees(mc.player.getYaw() - 45.0F) + random;
            float pitch = mc.player.getPitch() + random;
            ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(yaw, pitch), 360.0D);
        }
    }

    private boolean shouldFastFallOnTick(int tick) {
        return switch (tick) {
            case 1 -> tick1.getBoolean();
            case 2 -> tick2.getBoolean();
            case 3 -> tick3.getBoolean();
            case 4 -> tick4.getBoolean();
            case 5 -> tick5.getBoolean();
            default -> false;
        };
    }

    private void sendFastFallPackets() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        for (int i = 0; i < packetCount.getInt(); i++) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
        mc.options.sneakKey.setPressed(false);
        mc.player.jump();
        moveFlying(grimMoveFlying.getDouble() / 10.0D);
        for (int i = 0; i < ticks.getInt(); i++) {
            mc.player.tick();
        }
        pendingSkipTicks += skipTicks.getDouble();
        log("fall");
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

    private boolean isModuleEnabled(String name) {
        Module module = ModuleManager.INSTANCE.getByName(name);
        return module != null && module.isEnabled();
    }

    private boolean hasKillAuraTarget() {
        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        return killAura != null && killAura.isEnabled() && killAura.getTarget() != null;
    }

    private void log(String message) {
        if (logging.getBoolean() && mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
