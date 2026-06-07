package cn.gardenia.client.module.render;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.item.SwordItem;

public class SwordBlockModule extends Module {

    private static SwordBlockModule INSTANCE;

    private boolean blocking;

    public SwordBlockModule() {
        super("SwordBlock", "1.8.9 style sword blocking animation", Category.RENDER);
        INSTANCE = this;
    }

    public static SwordBlockModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            blocking = false;
            return;
        }
        boolean holdingSword = mc.player.getMainHandStack().getItem() instanceof SwordItem;
        boolean pressing = mc.options.useKey.isPressed();
        blocking = holdingSword && pressing;
    }

    public boolean isBlocking() {
        return blocking && isEnabled();
    }
}
