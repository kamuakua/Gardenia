package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.block.SkullBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Consumer;

public class AutoHealModule extends Module {
    private final Setting<Boolean> speedCheck = new Setting<>("Speed Check", "Skip healing while Speed is active", true);
    private final Setting<Boolean> regenCheck = new Setting<>("Regen Check", "Skip healing while Regeneration is active", true);
    private final Setting<Double> delay = rangedDouble("Delay", "Heal delay in milliseconds", 500.0D, 300.0D, 1000.0D, 1.0D);
    private final Setting<Double> health = rangedDouble("Health Percent", "Health percentage threshold", 0.5D, 0.0D, 1.0D, 0.05D);
    private final Setting<String> mode = new Setting<>("Mode", "Healing item mode", "Soup", new String[]{"Soup", "Head"});
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private long lastHealTime;
    private Integer switchBackSlot;

    public AutoHealModule() {
        super("AutoHeal", "Automatically heals you when low on health", Category.COMBAT);
        addSetting(speedCheck);
        addSetting(regenCheck);
        addSetting(delay);
        addSetting(health);
        addSetting(mode);
    }

    @Override
    public void onEnable() {
        lastHealTime = 0L;
        switchBackSlot = null;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        switchBackSlot = null;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (switchBackSlot != null) {
            mc.player.getInventory().selectedSlot = switchBackSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(switchBackSlot));
            switchBackSlot = null;
            return;
        }

        if (System.currentTimeMillis() - lastHealTime < delay.getDouble()) {
            return;
        }
        if (speedCheck.getBoolean() && mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            return;
        }
        if (regenCheck.getBoolean() && mc.player.hasStatusEffect(StatusEffects.REGENERATION)) {
            return;
        }
        if (mc.player.getHealth() / mc.player.getMaxHealth() >= health.getDouble()) {
            return;
        }

        int slot = "Soup".equals(mode.getString()) ? findSoupSlot() : findHeadSlot();
        if (slot == -1) {
            return;
        }

        switchBackSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            if ("Soup".equals(mode.getString())) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            }
        }
        lastHealTime = System.currentTimeMillis();
    }

    private int findSoupSlot() {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.MUSHROOM_STEW)) {
                return slot;
            }
        }
        return -1;
    }

    private int findHeadSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
                return slot;
            }
        }
        return -1;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
