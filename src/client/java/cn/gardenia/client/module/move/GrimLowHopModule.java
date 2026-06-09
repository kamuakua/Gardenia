package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.util.PlayerInput;

import java.util.function.Consumer;

public class GrimLowHopModule extends Module {
    public static GrimLowHopModule INSTANCE;

    private final Setting<Boolean> logging = new Setting<>("Logging", "Print debug state to chat", false);
    private final Setting<Double> startTicks = rangedDouble("Start Tick", "Air tick before acceleration starts", 2.0D, 0.0D, 10.0D, 1.0D);
    private final Setting<Double> skipTicks = rangedDouble("Skip Ticks", "Skipped tick budget mirror", 2.0D, 1.0D, 10.0D, 1.0D);
    private final Setting<Double> ticks = rangedDouble("Ticks", "Extra player ticks to run once airborne", 3.0D, 1.0D, 10.0D, 1.0D);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private int airTicks;
    private double pendingSkipTicks;
    private boolean canTimer;
    private boolean timer;

    public GrimLowHopModule() {
        super("GrimLowHop", "Movement speed adjustments with Grim low-hop mode", Category.MOVEMENT);
        INSTANCE = this;
        addSetting(logging);
        addSetting(startTicks);
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
        canTimer = false;
        timer = false;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        airTicks = 0;
        pendingSkipTicks = 0.0D;
        canTimer = false;
        timer = false;
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }

        if (mc.player.isOnGround()) {
            airTicks = 0;
            canTimer = false;
            timer = false;
            pendingSkipTicks = 0.0D;
        } else {
            airTicks++;
        }

        if (airTicks >= startTicks.getInt() && !canTimer) {
            pendingSkipTicks += skipTicks.getDouble();
            canTimer = true;
            timer = false;
            log("GrimLowHop queued skip ticks: " + pendingSkipTicks);
        }

        if (canTimer && !timer) {
            timer = true;
            for (int i = 0; i < ticks.getInt(); i++) {
                mc.player.tick();
            }
            pendingSkipTicks = Math.max(0.0D, pendingSkipTicks - ticks.getInt());
        }
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
