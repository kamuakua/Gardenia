package cn.gardenia.client.module;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.module.misc.NewInvManagerModule;

import java.util.function.Consumer;

public class SprintModule extends Module {
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;

    public SprintModule() {
        super("Sprint", "Automatically sprints", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerMotionEvent.class, motionListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.options == null) {
            return;
        }
        if (NewInvManagerModule.isSuppressingSprint()) {
            mc.options.sprintKey.setPressed(false);
            return;
        }
        mc.options.sprintKey.setPressed(true);
        mc.options.getSprintToggled().setValue(false);
    }
}
