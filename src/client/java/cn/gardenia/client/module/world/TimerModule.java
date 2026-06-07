package cn.gardenia.client.module.world;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.game.GameTickEvent;
import java.util.function.Consumer;

public class TimerModule extends Module {
    private final Setting<Double> speed = new Setting<>("Speed", "Game speed multiplier", 2.0);
    private final Consumer<GameTickEvent> tickListener;
    private float prevTickDelta = 1.0f;

    public TimerModule() {
        super("Timer", "Speed up game time", Category.WORLD);
        addSetting(speed);
        this.tickListener = this::onGameTick;
    }

    @Override
    public void onEnable() {
        subscribe(GameTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GameTickEvent.class, tickListener);
    }

    private void onGameTick(GameTickEvent event) {
        if (mc.world == null) return;
        double multiplier = speed.getDouble();
        if (multiplier <= 0) multiplier = 0.1;
        if (multiplier > 10) multiplier = 10;
    }
}