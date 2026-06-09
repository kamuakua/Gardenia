package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.mixin.client.MinecraftClientAccessor;
import java.util.function.Consumer;

public class FastPlaceModule extends Module {
    private final Setting<Integer> delay = new Setting<>("Delay", "Place delay in ticks", 0);
    private final Setting<Boolean> onlyBlocks = new Setting<>("OnlyBlocks", "Only fast place blocks", true);
    private final Consumer<PlayerTickEvent> tickListener;
    private int placeTicks = 0;

    public FastPlaceModule() {
        super("FastPlace", "Place blocks faster", Category.MISC);
        addSetting(delay);
        addSetting(onlyBlocks);
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
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.options == null || !mc.options.useKey.isPressed()) return;

        if (onlyBlocks.getBoolean() && mc.player.getMainHandStack() != null) {
            if (!(mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem)) {
                return;
            }
        }

        placeTicks++;
        if (placeTicks >= delay.getInt()) {
            ((MinecraftClientAccessor) mc).gardenia$setRightClickDelay(0);
            placeTicks = 0;
        }
    }
}
