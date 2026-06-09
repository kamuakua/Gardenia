package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;

import java.util.function.Consumer;

public class SafeWalkModule extends Module {
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;

    public SafeWalkModule() {
        super("SafeWalk", "Prevents you from falling off blocks", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerMotionEvent.class, motionListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.options == null) {
            return;
        }
        mc.options.sneakKey.setPressed(mc.player.isOnGround() && MovementEdgeUtil.isOnBlockEdge(0.3F));
    }
}
