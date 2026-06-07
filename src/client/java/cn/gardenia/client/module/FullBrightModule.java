package cn.gardenia.client.module;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import java.util.function.Consumer;

public class FullBrightModule extends Module {
    private double previousGamma;
    public final Setting<Double> brightness = new Setting<>("Brightness", "Night vision brightness strength", 1.0);
    private final Consumer<PlayerTickEvent> tickListener;

    public FullBrightModule() {
        super("FullBright", "Maximum brightness in all areas", Category.RENDER);
        brightness.setRange(0.0, 1.0, 0.1);
        addSetting(brightness);
        this.tickListener = e -> {
            if (e.isPre() && mc.options != null) {
                mc.options.getGamma().setValue(1.0);
            }
        };
    }

    @Override
    public void onEnable() {
        if (mc.options == null) return;
        previousGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(1.0);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        if (mc.options == null) return;
        mc.options.getGamma().setValue(previousGamma);
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    public float getBrightness() {
        return (float) brightness.getDouble();
    }
}
