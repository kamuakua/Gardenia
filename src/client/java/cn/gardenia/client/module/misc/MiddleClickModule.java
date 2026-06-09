package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.input.MouseEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.gui.notification.Notification;
import cn.gardenia.client.gui.notification.NotificationManager;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.social.FriendManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

import java.util.function.Consumer;

public class MiddleClickModule extends Module {
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<MouseEvent> mouseListener = this::onMouse;

    private boolean clicked;

    public MiddleClickModule() {
        super("Middle Click", "Middle click friends or pearls", Category.MISC);
    }

    @Override
    public void onEnable() {
        clicked = false;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(MouseEvent.class, mouseListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(MouseEvent.class, mouseListener);
        clicked = false;
    }

    private void onMouse(MouseEvent event) {
        if (event.getButton() == 2 && event.isRelease()) {
            clicked = true;
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || !clicked || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        clicked = false;

        if (mc.crosshairTarget instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof PlayerEntity player) {
            toggleFriend(player);
            return;
        }

        throwPearl();
    }

    private void toggleFriend(PlayerEntity player) {
        String name = player.getName().getString();
        if (FriendManager.isFriend(player)) {
            FriendManager.removeFriend(player);
            NotificationManager.INSTANCE.show(new Notification("Middle Click", "Removed " + name + " from friends", "notification_off", 3000));
        } else {
            FriendManager.addFriend(player);
            NotificationManager.INSTANCE.show(new Notification("Middle Click", "Added " + name + " as friend", "notification_on", 3000));
        }
    }

    private void throwPearl() {
        int originalSlot = mc.player.getInventory().selectedSlot;
        int pearlSlot = -1;
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) {
                pearlSlot = slot;
                break;
            }
        }

        if (pearlSlot == -1) {
            NotificationManager.INSTANCE.show(new Notification("Middle Click", "No pearls found", "notification_off", 3000));
            return;
        }

        mc.player.getInventory().selectedSlot = pearlSlot;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = originalSlot;
    }
}
