package cn.gardenia.client.module;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import java.util.function.Consumer;

public class SprintModule extends Module {
    private final Consumer<PlayerTickEvent> tickListener;

    public SprintModule() {
        super("Sprint", "Auto-sprint when moving forward", Category.PLAYER);
        this.tickListener = this::onPlayerTick;
    }

    @Override
    public void onEnable() {
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    private void onPlayerTick(PlayerTickEvent event) {
        if (!event.isPre()) return;
        if (mc.player == null) return;
        if (mc.player.forwardSpeed > 0 && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onTick() {
    }
}
