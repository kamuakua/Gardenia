package cn.gardenia.client.module.world;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.tool.player.GardeniaTickTimer;

public class TimerModule extends Module {
    private final Setting<Double> speed = rangedDouble("Speed", "Game speed multiplier", 2.0D, 0.1D, 10.0D, 0.1D);

    public TimerModule() {
        super("Timer", "Speed up game time", Category.WORLD);
        addSetting(speed);
    }

    @Override
    public void onEnable() {
        applySpeed();
    }

    @Override
    public void onDisable() {
        GardeniaTickTimer.resetPersistentMultiplier();
    }

    @Override
    public void onTick() {
        if (isEnabled()) {
            applySpeed();
        }
    }

    private void applySpeed() {
        GardeniaTickTimer.setPersistentMultiplier((float) speed.getDouble());
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
