package cn.gardenia.client.module.render;

import cn.gardenia.client.gui.notification.NotificationManager;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;

public class NotificationModule extends Module {

    private final Setting<Integer> displayDuration = new Setting<>(
            "Duration", "Notification display time (ms)", 2000, 800, 5000,
            v -> {}
    );
    private final Setting<Integer> maxStack = new Setting<>(
            "Max Stack", "Max concurrent notifications", 4, 1, 6,
            v -> {}
    );

    public NotificationModule() {
        super("Notifications", "Module toggle notifications", Category.RENDER, true);
        addSetting(displayDuration);
        addSetting(maxStack);
    }

    @Override
    public void onEnable() {
        NotificationManager.INSTANCE.setModuleEnabled(true);
    }

    @Override
    public void onDisable() {
        NotificationManager.INSTANCE.setModuleEnabled(false);
    }
}
