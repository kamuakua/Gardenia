package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.block.BlockBreakEvent;
import java.util.function.Consumer;

public class AutoToolModule extends Module {
    private final Setting<Boolean> preferSilkTouch = new Setting<>("PreferSilkTouch", "Prefer silk touch when available", false);
    private final Setting<Boolean> switchBack = new Setting<>("SwitchBack", "Switch back to previous slot", true);
    private final Consumer<BlockBreakEvent> breakListener;
    private int previousSlot = -1;

    public AutoToolModule() {
        super("AutoTool", "Automatically switch to best tool", Category.MISC);
        addSetting(preferSilkTouch);
        addSetting(switchBack);
        this.breakListener = this::onBlockBreak;
    }

    @Override
    public void onEnable() {
        subscribe(BlockBreakEvent.class, breakListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(BlockBreakEvent.class, breakListener);
    }

    private void onBlockBreak(BlockBreakEvent event) {
        if (mc.player == null || mc.world == null) return;

        net.minecraft.block.BlockState state = mc.world.getBlockState(event.getPos());
        if (state == null) return;

        int bestSlot = findBestTool(state);
        if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
            if (switchBack.getBoolean() && previousSlot == -1) {
                previousSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = bestSlot;
        }
    }

    private int findBestTool(net.minecraft.block.BlockState state) {
        if (mc.player == null) return -1;

        float bestSpeed = 1.0f;
        int bestSlot = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}