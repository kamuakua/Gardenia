package cn.gardenia.client.module.combat;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import java.util.function.Consumer;

public class AutoClickerModule extends Module {
    private final Setting<Integer> cps = new Setting<>("CPS", "Clicks per second", 12);
    private final Setting<Boolean> onlyWhenFocused = new Setting<>("OnlyWhenFocused", "Only click when looking at entity", true);
    private final Consumer<PlayerTickEvent> tickListener;
    private long lastClickTime = 0;

    public AutoClickerModule() {
        super("AutoClicker", "Automatically clicks when holding attack key", Category.COMBAT);
        addSetting(cps);
        addSetting(onlyWhenFocused);
        this.tickListener = this::onTickEvent;
    }

    @Override
    public void onEnable() {
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    private void onTickEvent(PlayerTickEvent event) {
        if (!event.isPre()) return;
        if (mc.player == null || mc.options == null) return;
        if (!mc.options.attackKey.isPressed()) return;

        if (onlyWhenFocused.getBoolean() && mc.targetedEntity == null) return;

        int delay = 1000 / cps.getInt();
        long now = System.currentTimeMillis();
        if (now - lastClickTime >= delay) {
            lastClickTime = now;
            if (mc.interactionManager != null && mc.crosshairTarget != null) {
                mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }
}