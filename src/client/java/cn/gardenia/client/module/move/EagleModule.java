package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.mixin.client.MinecraftClientAccessor;
import net.minecraft.item.BlockItem;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class EagleModule extends Module {
    private final Setting<Integer> minDelay = new Setting<>("MinDelay (Ticks)", "Minimum sneak delay", 2, 0, 10);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelay (Ticks)", "Maximum sneak delay", 3, 0, 10);
    private final Setting<Boolean> onlyBackwards = new Setting<>("OnlyBackwards", "Only sneak while moving backwards", false);
    private final Setting<Boolean> onlyWithBlocks = new Setting<>("OnlyWithBlocks", "Only work while holding blocks", false);
    private final Setting<Boolean> fastPlace = new Setting<>("FastPlace", "Reduce block place delay while eagle is active", false);
    private final Setting<Double> cps = rangedDouble("CPS", "FastPlace clicks per second", 10.0D, 5.0D, 20.0D, 1.0D);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private float lastForward;
    private int sneakUntilTick;
    private double placeAccumulator;

    public EagleModule() {
        super("Eagle", "Auto-sneak near block edges", Category.MOVEMENT);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(onlyBackwards);
        addSetting(onlyWithBlocks);
        addSetting(fastPlace);
        addSetting(cps);
    }

    @Override
    public void onEnable() {
        lastForward = 0.0F;
        sneakUntilTick = 0;
        placeAccumulator = 0.0D;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
        }
        lastForward = 0.0F;
        sneakUntilTick = 0;
        placeAccumulator = 0.0D;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.options == null) {
            return;
        }

        if (mc.player.forwardSpeed != 0.0F) {
            lastForward = mc.player.forwardSpeed;
        }

        if (!canEagle()) {
            mc.options.sneakKey.setPressed(false);
            placeAccumulator = 0.0D;
            return;
        }

        boolean closeToEdge = mc.player.isOnGround()
                && !mc.player.getAbilities().flying
                && MovementEdgeUtil.isOnBlockEdge(0.3F);
        if (closeToEdge) {
            sneakUntilTick = mc.player.age + randomDelayTicks();
        }
        mc.options.sneakKey.setPressed(closeToEdge || mc.player.age < sneakUntilTick);
        handleFastPlace();
    }

    private boolean canEagle() {
        if (onlyBackwards.getBoolean() && lastForward > 0.0F) {
            return false;
        }
        return !onlyWithBlocks.getBoolean() || isHoldingBlock();
    }

    private boolean isHoldingBlock() {
        return mc.player != null && mc.player.getMainHandStack().getItem() instanceof BlockItem;
    }

    private int randomDelayTicks() {
        int min = Math.min(minDelay.getInt(), maxDelay.getInt());
        int max = Math.max(minDelay.getInt(), maxDelay.getInt());
        return min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void handleFastPlace() {
        if (!fastPlace.getBoolean() || mc.options == null || !mc.options.useKey.isPressed() || !isHoldingBlock()) {
            placeAccumulator = 0.0D;
            return;
        }
        placeAccumulator += cps.getDouble() / 20.0D;
        if (placeAccumulator >= 1.0D) {
            ((MinecraftClientAccessor) mc).gardenia$setRightClickDelay(0);
            placeAccumulator -= 1.0D;
        }
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
