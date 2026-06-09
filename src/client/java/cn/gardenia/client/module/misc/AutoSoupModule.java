package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.function.Consumer;

public class AutoSoupModule extends Module {
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private Integer backSlot;

    public AutoSoupModule() {
        super("AutoSoup", "Automatically uses mushroom stew when health is low", Category.MISC);
    }

    @Override
    public void onEnable() {
        backSlot = null;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        backSlot = null;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (backSlot != null) {
            mc.player.getInventory().selectedSlot = backSlot;
            backSlot = null;
            return;
        }

        if (mc.player.age % 10 != 0 || mc.player.getHealth() >= mc.player.getMaxHealth() / 2.0F) {
            return;
        }

        int soupSlot = findSoupHotbarSlot();
        if (soupSlot == -1) {
            return;
        }

        backSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = soupSlot;
        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private int findSoupHotbarSlot() {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.MUSHROOM_STEW)) {
                return slot;
            }
        }
        return -1;
    }
}
