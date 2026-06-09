package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.mixin.client.entity.LivingEntityAccessor;

import java.util.function.Consumer;

public class NoJumpDelayModule extends Module {
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;

    public NoJumpDelayModule() {
        super("NoJumpDelay", "Removes the delay when jumping", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerMotionEvent.class, motionListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null) {
            return;
        }
        ((LivingEntityAccessor) mc.player).gardenia$setNoJumpDelay(0);
    }
}
