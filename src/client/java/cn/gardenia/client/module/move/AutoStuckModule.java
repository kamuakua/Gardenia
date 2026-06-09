package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.network.PacketReceiveEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Consumer;

public class AutoStuckModule extends Module {
    private final Setting<Double> fallDistance = rangedDouble("Fall Distance", "Fall distance before enabling Stuck", 10.0D, 3.0D, 15.0D, 0.1D);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PacketReceiveEvent> packetListener = this::onPacket;

    private long lastResetTime;

    public AutoStuckModule() {
        super("AutoStuck", "Automatically enable Stuck when you are over void", Category.MOVEMENT);
        addSetting(fallDistance);
    }

    @Override
    public void onEnable() {
        resetTimer();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PacketReceiveEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PacketReceiveEvent.class, packetListener);
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        StuckModule stuck = ModuleManager.INSTANCE.getByClass(StuckModule.class);
        if (stuck == null) {
            return;
        }

        if (stuck.isEnabled()) {
            resetTimer();
        }

        boolean hasPearl = false;
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) {
                hasPearl = true;
                break;
            }
        }

        boolean shouldTrigger = ((hasPearl && mc.player.fallDistance > fallDistance.getDouble())
                || mc.player.getY() + mc.player.getVelocity().y < -50.0D)
                && isOverVoid()
                && !mc.player.isOnGround()
                && System.currentTimeMillis() - lastResetTime >= 1000L;

        if (shouldTrigger && !stuck.isEnabled()) {
            stuck.toggle();
        }
    }

    private void onPacket(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            resetTimer();
        }
    }

    private boolean isOverVoid() {
        Vec3d start = mc.player.getPos();
        Vec3d end = new Vec3d(start.x, mc.world.getBottomY() - 2.0D, start.z);
        HitResult hit = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void resetTimer() {
        lastResetTime = System.currentTimeMillis();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
