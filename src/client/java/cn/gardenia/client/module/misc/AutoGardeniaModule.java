package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AutoGardeniaModule extends Module {
    private final Setting<Boolean> autoScreenshot = new Setting<>("Auto Screenshot", "Save a screenshot after a victory title", true);
    private final Setting<Boolean> autoPlay = new Setting<>("Auto Play", "Use the center hotbar slot after a victory title", true);
    private final Setting<Boolean> victoryClose = new Setting<>("Victory Close", "Disconnect after a victory title", false);

    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    public AutoGardeniaModule() {
        super("AutoGardenia", "Automatically handles victory title actions", Category.MISC);
        addSetting(autoScreenshot);
        addSetting(autoPlay);
        addSetting(victoryClose);
    }

    @Override
    public void onEnable() {
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GlobalPacketEvent.class, packetListener);
    }

    private void onPacket(GlobalPacketEvent event) {
        if (!event.isReceive() || mc.player == null || mc.world == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (!(packet instanceof TitleS2CPacket titlePacket)) {
            return;
        }

        String title = titlePacket.text().getString();
        boolean win = title.contains("胜利");
        if (!win) {
            return;
        }

        if (autoScreenshot.getBoolean()) {
            scheduleScreenshot();
        }

        if (victoryClose.getBoolean() && (title.contains("胜利！") || title.contains("胜利!"))) {
            mc.execute(() -> {
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().getConnection().disconnect(Text.literal("Victory Close"));
                    mc.setScreen(new TitleScreen());
                }
            });
        }

        if (autoPlay.getBoolean()) {
            scheduleAutoPlay();
        }
    }

    private void scheduleScreenshot() {
        CompletableFuture.runAsync(() -> sleep(1, TimeUnit.SECONDS)).thenRun(() -> mc.execute(() -> {
            if (mc.player == null || mc.world == null) {
                return;
            }
            ScreenshotRecorder.saveScreenshot(
                    mc.runDirectory,
                    mc.getFramebuffer(),
                    message -> {
                        if (mc.player != null) {
                            mc.player.sendMessage(message, false);
                        }
                    }
            );
        }));
    }

    private void scheduleAutoPlay() {
        CompletableFuture.runAsync(() -> sleep(500, TimeUnit.MILLISECONDS)).thenRun(() -> mc.execute(() -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null) {
                return;
            }

            int originalSlot = mc.player.getInventory().selectedSlot;
            int targetSlot = 4;
            selectSlot(targetSlot);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (originalSlot != targetSlot) {
                selectSlot(originalSlot);
            }
        }));
    }

    private void selectSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void sleep(long duration, TimeUnit unit) {
        try {
            unit.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
