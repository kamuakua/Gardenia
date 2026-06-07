package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.movement.StayingOnGroundSurfaceEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.LookAtS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Velocity extends Module {
    public static Velocity INSTANCE;

    private final Setting<String> mode = new Setting<>("Mode", "Velocity mode", "Legit", new String[]{"Legit", "Reduce"});
    private final Setting<Double> attacks = rangedDouble("AttackCounts", "Attack count gate for NoXZ mode", 3.0, 1.0, 5.0, 1.0);
    private final Setting<Double> alinkTime = rangedDouble("MaxAlinkTime (ms)", "Maximum link time for NoXZ mode", 5000.0, 50.0, 10000.0, 50.0);

    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final SmoothProgress progress = new SmoothProgress(0.0F, 0.2F);
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerTickEvent> tickListener = this::onPreTick;
    private final Consumer<RenderHudEvent> renderListener = this::onRender;

    private boolean lag;
    private boolean jump;
    private int aliveTicks;
    private VelocityStage stage = VelocityStage.NONE;

    public Velocity() {
        super("AntiKnockBack", "Fuck 妖怪猫咪的 Ass", Category.COMBAT);
        INSTANCE = this;
        addSetting(mode);
        addSetting(attacks);
        addSetting(alinkTime);
    }

    public boolean shouldStopBacktrack() {
        return false;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        reset();
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(RenderHudEvent.class, renderListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(RenderHudEvent.class, renderListener);
        reset();
        clear(true);
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return INSTANCE.onMoveInput(input);
    }

    public static boolean handleStayingOnGroundSurface(boolean stay) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return stay;
        }
        StayingOnGroundSurfaceEvent event = new StayingOnGroundSurfaceEvent(stay);
        INSTANCE.onStayingOnGround(event);
        return event.shouldStay();
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null) {
            return;
        }

        String currentMode = mode.getString();
        if (currentMode.equals("NoXZ") || currentMode.equals("Reduce") && (!mc.player.isOnGround() || stage == VelocityStage.DELAY)) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof PlayerPositionLookS2CPacket && stage == VelocityStage.NONE) {
                lag = true;
                return;
            }

            if (packet instanceof EntityVelocityUpdateS2CPacket motion && motion.getEntityId() == mc.player.getId()) {
                if (stage == VelocityStage.NONE) {
                    if (!lag) {
                        stage = VelocityStage.DELAY;
                        aliveTicks = 0;
                        event.cancel();
                        packets.add(packet);
                    } else {
                        lag = false;
                    }
                } else {
                    packets.add(packet);
                    event.cancel();
                }
                return;
            }

            if (stage != VelocityStage.NONE && event.isReceive()) {
                if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof LookAtS2CPacket) {
                    stage = VelocityStage.LAG;
                    return;
                }

                if (packet instanceof DisconnectS2CPacket || packet instanceof PlayerRespawnS2CPacket) {
                    clear(false);
                    return;
                }

                if (packet instanceof CommonPingS2CPacket || packet instanceof EntityS2CPacket || packet instanceof EntityPositionSyncS2CPacket) {
                    packets.add(packet);
                    event.cancel();
                }
            }
        } else if (currentMode.equals("Legit") || currentMode.equals("Reduce") && mc.player.isOnGround()) {
            Packet<?> motionPacket = event.getPacket();
            if (motionPacket instanceof EntityVelocityUpdateS2CPacket motion && motion.getEntityId() == mc.player.getId()) {
                jump = true;
            }
        }
    }

    private void onPreTick(PlayerTickEvent event) {
        if (mc.player == null || !event.isPre()) {
            return;
        }

        String curMode = mode.getString();
        if (curMode.equals("Reduce") && stage == VelocityStage.DELAY) {
            progress.target = MathHelper.clamp((float) aliveTicks / 20.0F * 100.0F, 0.0F, 100.0F);
        } else {
            progress.target = 0.0F;
        }

        if (stage == VelocityStage.DELAY || stage == VelocityStage.LAG) {
            boolean shouldRelease = false;
            if (!curMode.equals("Reduce")) {
                if (curMode.equals("NoXZ") && System.currentTimeMillis() > 0L && stage == VelocityStage.LAG) {
                    shouldRelease = true;
                }
            } else {
                ++aliveTicks;
                if (aliveTicks >= 24 || mc.player.isOnGround() && aliveTicks > 2 || stage == VelocityStage.LAG) {
                    shouldRelease = true;
                }
            }

            if (shouldRelease) {
                clear(true);
                if (curMode.equals("Reduce") && mc.player.isOnGround()) {
                    jump = true;
                }

                stage = VelocityStage.NONE;
                aliveTicks = 0;
            }
        }
    }

    private void onStayingOnGround(StayingOnGroundSurfaceEvent event) {
        if (mc.player == null) {
            return;
        }
        if ("Reduce".equals(mode.getString()) && stage == VelocityStage.DELAY) {
            event.setStay(false);
        }

        if (jump && mc.player.isOnGround()) {
            event.setStay(true);
        }
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        if (jump && mc.player != null && mc.player.isOnGround() && isMoving(input)) {
            jump = false;
            return new PlayerInput(
                    input.forward(),
                    input.backward(),
                    input.left(),
                    input.right(),
                    true,
                    input.sneak(),
                    input.sprint()
            );
        }
        return input;
    }

    private boolean isMoving(PlayerInput input) {
        return input.left()
                || input.right()
                || input.forward()
                || input.backward()
                || input.jump()
                || mc.options.jumpKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()
                || mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed();
    }

    private void onRender(RenderHudEvent event) {
        if (stage == VelocityStage.NONE && progress.value < 0.1F) {
            return;
        }

        progress.update();
        float width = 100.0F;
        float height = 5.0F;
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        float x = screenWidth / 2.0F - width / 2.0F;
        float y = screenHeight / 2.0F + 15.0F;
        float progressWidth = MathHelper.clamp(progress.value, 0.0F, width);

        boolean startedFrame = false;
        if (!NanoVGManager.INSTANCE.isFrameActive()) {
            if (!NanoVGManager.INSTANCE.isInitialized()) {
                NanoVGManager.INSTANCE.init();
            }
            NanoVGManager.INSTANCE.beginFrame(
                    mc.getWindow().getScaledWidth(),
                    mc.getWindow().getScaledHeight(),
                    (float) mc.getWindow().getScaleFactor()
            );
            startedFrame = true;
        }

        NanoVGRender.drawRoundedRect(x, y, width, height, 2.0F, 0x6E000000);
        if (progress.value > 0.1F) {
            NanoVGRender.drawRoundedRect(x, y, progressWidth, height, 2.0F, hudSyncColor());
        }

        if (startedFrame) {
            NanoVGManager.INSTANCE.endFrame();
        }
    }

    private int hudSyncColor() {
        float phase = (System.currentTimeMillis() / 20L) % 50L;
        float t = phase <= 25.0F ? phase / 25.0F : (50.0F - phase) / 25.0F;
        return Theme.blend(Theme.ACCENT, Theme.ACCENT_HOVER, t);
    }

    public void clear(boolean handle) {
        lag = false;
        if (!handle) {
            packets.clear();
            return;
        }

        while (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();
            if (packet != null && mc.getNetworkHandler() != null) {
                handle(packet, mc.getNetworkHandler());
            }
        }
    }

    private void reset() {
        jump = false;
        lag = false;
        aliveTicks = 0;
        stage = VelocityStage.NONE;
        packets.clear();
        progress.value = 0.0F;
        progress.target = 0.0F;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(Packet<?> packet, PacketListener listener) {
        ((Packet) packet).apply(listener);
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private enum VelocityStage {
        NONE,
        DELAY,
        LAG
    }

    private static class SmoothProgress {
        private float value;
        private float target;
        private final float speed;

        private SmoothProgress(float value, float speed) {
            this.value = value;
            this.target = value;
            this.speed = speed;
        }

        private void update() {
            value += (target - value) * speed;
        }
    }
}
