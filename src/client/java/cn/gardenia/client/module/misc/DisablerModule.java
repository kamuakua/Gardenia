package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.Text;

import java.util.Random;
import java.util.function.Consumer;

public class DisablerModule extends Module {
    private final Setting<Boolean> logging = new Setting<>("Logging", "Log Disabler actions to chat", false);
    private final Setting<Boolean> acaFastSwitch = new Setting<>("ACAFastSwitch", "Send intermediate slot packets while switching", true);
    private final Setting<Boolean> acaInventoryFrequency = new Setting<>("ACAInventoryFrequency", "Delay too-fast inventory close packets", false);
    private final Setting<Boolean> acaAimStep = new Setting<>("ACAAimStep", "Reserved for ACA aim-step rotation correction", false);
    private final Setting<Boolean> acaPerfectRotation = new Setting<>("ACAPerfectRotation", "Reserved for ACA perfect-rotation correction", false);
    private final Setting<Boolean> themisBlink = new Setting<>("ThemisBlink", "Periodically send a pong while tracking movement/pong packets", false);
    private final Setting<Boolean> onlyRemoteServer = new Setting<>("Only Remote Server", "Disable handling in singleplayer", false);
    private final Setting<Boolean> grimDuplicateRotPlace = new Setting<>("Grim-DuplicateRotPlace", "Track duplicate rotation before placement", false);
    private final Setting<Boolean> badPacketsA = new Setting<>("BadPackets-A", "Cancel duplicate selected-slot packets", false);
    private final Setting<Boolean> badPacketsD = new Setting<>("BadPackets-D", "Cancel invalid pitch packets", false);
    private final Setting<Boolean> badPacketsF = new Setting<>("BadPackets-F", "Cancel duplicate sprint command packets", false);
    private final Setting<Boolean> badPacketsG = new Setting<>("BadPackets-G", "Cancel duplicate sneak command packets", false);
    private final Setting<Boolean> badPacketY = new Setting<>("BadPacket-Y", "Clamp invalid selected-slot packets", false);

    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Random random = new Random();

    private int lastSlot = -1;
    private boolean lastSprinting;
    private boolean lastSneaking;
    private long inventoryOpenTime;
    private boolean inventoryOpen;
    private CloseHandledScreenC2SPacket storedClosePacket;
    private long inventoryCloseDelay;
    private long inventoryCloseDelayStartedAt;
    private long themisBlinkLastSend = System.currentTimeMillis();
    private int themisBlinkCount;
    private float currentYaw;
    private float currentPitch;
    private float currentYawDiff;
    private float currentPitchDiff;
    private float lastPlacedYawDiff;
    private float lastPlacedPitchDiff;
    private boolean rotated;
    private boolean replaying;

    public DisablerModule() {
        super("Disabler", "Disables selected packet checks", Category.MISC);
        addSetting(logging);
        addSetting(acaFastSwitch);
        addSetting(acaInventoryFrequency);
        addSetting(acaAimStep);
        addSetting(acaPerfectRotation);
        addSetting(themisBlink);
        addSetting(onlyRemoteServer);
        addSetting(grimDuplicateRotPlace);
        addSetting(badPacketsA);
        addSetting(badPacketsD);
        addSetting(badPacketsF);
        addSetting(badPacketsG);
        addSetting(badPacketY);
    }

    @Override
    public void onEnable() {
        reset();
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(GlobalPacketEvent.class, packetListener);
        reset();
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || replaying || event.isCancelled()) {
            return;
        }
        if (onlyRemoteServer.getBoolean() && mc.isInSingleplayer()) {
            return;
        }
        if (mc.player.isSpectator() || !mc.player.isAlive() || mc.currentScreen instanceof DownloadingTerrainScreen) {
            reset();
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof GameJoinS2CPacket) {
            reset();
            return;
        }

        if (event.isReceive()) {
            if (packet instanceof OpenScreenS2CPacket) {
                inventoryOpenTime = System.currentTimeMillis();
                inventoryOpen = true;
                log("Inventory opened.");
            }
            return;
        }

        if (!event.isSend()) {
            return;
        }

        releaseStoredCloseIfReady();

        if (badPacketsA.getBoolean() && packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
            if (slotPacket.getSelectedSlot() == lastSlot) {
                event.cancel();
                log("BadPackets-A: Cancelled duplicate slot packet.");
                return;
            }
            lastSlot = slotPacket.getSelectedSlot();
        }

        if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
            int slot = slotPacket.getSelectedSlot();
            if (acaFastSwitch.getBoolean() && lastSlot != -1 && slot != lastSlot) {
                sendIntermediateSlots(lastSlot, slot);
            }
            lastSlot = slot;
        }

        if (acaInventoryFrequency.getBoolean() && packet instanceof CloseHandledScreenC2SPacket closePacket) {
            if (inventoryOpen) {
                long elapsed = System.currentTimeMillis() - inventoryOpenTime;
                if (elapsed <= 150L) {
                    event.cancel();
                    storedClosePacket = closePacket;
                    inventoryCloseDelay = 151L - elapsed;
                    inventoryCloseDelayStartedAt = System.currentTimeMillis();
                    inventoryOpen = false;
                    log("InventoryFrequency: Stored close packet for " + inventoryCloseDelay + "ms.");
                    return;
                }
                inventoryOpen = false;
            }
        }

        if (themisBlink.getBoolean()) {
            if (System.currentTimeMillis() - themisBlinkLastSend > 200L) {
                if (themisBlinkCount == 0) {
                    sendNoEvent(new CommonPongC2SPacket(0));
                }
                themisBlinkLastSend = System.currentTimeMillis();
                themisBlinkCount = 0;
            }
            if (packet instanceof PlayerMoveC2SPacket || packet instanceof CommonPongC2SPacket) {
                themisBlinkCount++;
            }
        }

        if (badPacketsD.getBoolean() && packet instanceof PlayerMoveC2SPacket movePacket && movePacket.changesLook()) {
            float pitch = movePacket.getPitch(0.0F);
            if (Math.abs(pitch) > 90.0F) {
                event.cancel();
                log("BadPackets-D: Cancelled invalid pitch packet.");
                return;
            }
        }

        if ((badPacketsF.getBoolean() || badPacketsG.getBoolean()) && packet instanceof ClientCommandC2SPacket commandPacket) {
            handleCommandPacket(event, commandPacket);
            if (event.isCancelled()) {
                return;
            }
        }

        if (badPacketY.getBoolean() && packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
            int slotValue = slotPacket.getSelectedSlot();
            if (slotValue < 0 || slotValue > 8) {
                int clampedSlot = Math.max(0, Math.min(8, slotValue));
                event.setPacket(new UpdateSelectedSlotC2SPacket(clampedSlot));
            }
        }

        if (grimDuplicateRotPlace.getBoolean() && !event.isCancelled()) {
            trackDuplicateRotPlace(packet);
        }
    }

    private void handleCommandPacket(GlobalPacketEvent event, ClientCommandC2SPacket commandPacket) {
        ClientCommandC2SPacket.Mode mode = commandPacket.getMode();

        if (badPacketsF.getBoolean()) {
            if (mode == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                if (lastSprinting) {
                    event.cancel();
                    log("BadPackets-F: Cancelled duplicate start sprint packet.");
                } else {
                    lastSprinting = true;
                }
            } else if (mode == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                if (!lastSprinting) {
                    event.cancel();
                    log("BadPackets-F: Cancelled duplicate stop sprint packet.");
                } else {
                    lastSprinting = false;
                }
            }
        }

        if (badPacketsG.getBoolean()) {
            if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
                if (lastSneaking) {
                    event.cancel();
                    log("BadPackets-G: Cancelled duplicate start sneak packet.");
                } else {
                    lastSneaking = true;
                }
            } else if (mode == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
                if (!lastSneaking) {
                    event.cancel();
                    log("BadPackets-G: Cancelled duplicate stop sneak packet.");
                } else {
                    lastSneaking = false;
                }
            }
        }
    }

    private void trackDuplicateRotPlace(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket movePacket && movePacket.changesLook()) {
            float previousYaw = currentYaw;
            float previousPitch = currentPitch;
            currentYaw = movePacket.getYaw(previousYaw);
            currentPitch = movePacket.getPitch(previousPitch);
            currentYawDiff = Math.abs(currentYaw - previousYaw);
            currentPitchDiff = Math.abs(currentPitch - previousPitch);
            rotated = true;

            if (currentYawDiff > 2.0F && Math.abs(currentYawDiff - lastPlacedYawDiff) < 1.0E-4F) {
                log("DuplicateRotPlace: Detected duplicate yaw diff, jitter pending " + random.nextInt(10));
            }
            if (currentPitchDiff > 2.0F && Math.abs(currentPitchDiff - lastPlacedPitchDiff) < 1.0E-4F) {
                log("DuplicateRotPlace: Detected duplicate pitch diff, jitter pending " + random.nextInt(10));
            }
        } else if (isUseItemOnPacket(packet) && rotated) {
            lastPlacedYawDiff = currentYawDiff;
            lastPlacedPitchDiff = currentPitchDiff;
            rotated = false;
        }
    }

    private boolean isUseItemOnPacket(Packet<?> packet) {
        return packet.getClass().getName().endsWith("PlayerInteractBlockC2SPacket");
    }

    private void releaseStoredCloseIfReady() {
        if (storedClosePacket != null && System.currentTimeMillis() - inventoryCloseDelayStartedAt >= inventoryCloseDelay) {
            sendNoEvent(storedClosePacket);
            storedClosePacket = null;
            log("InventoryFrequency: Released stored close packet.");
        }
    }

    private void sendIntermediateSlots(int from, int to) {
        int diff = Math.abs(from - to);
        if (diff <= 1 || isWrapAroundSlot(from, to)) {
            return;
        }

        int step = from > to ? -1 : 1;
        for (int slot = from + step; slot != to; slot += step) {
            if (slot >= 0 && slot <= 8) {
                sendNoEvent(new UpdateSelectedSlotC2SPacket(slot));
                log("ACAFastSwitch: Sent intermediate slot " + slot + ".");
            }
        }
    }

    private boolean isWrapAroundSlot(int from, int to) {
        return from == 0 && to == 8 || from == 8 && to == 0;
    }

    private void reset() {
        rotated = false;
        lastSlot = -1;
        lastSprinting = false;
        lastSneaking = false;
        inventoryOpenTime = 0L;
        inventoryOpen = false;
        storedClosePacket = null;
        inventoryCloseDelay = 0L;
        inventoryCloseDelayStartedAt = 0L;
        themisBlinkLastSend = System.currentTimeMillis();
        themisBlinkCount = 0;
        currentYaw = 0.0F;
        currentPitch = 0.0F;
        currentYawDiff = 0.0F;
        currentPitchDiff = 0.0F;
        lastPlacedYawDiff = 0.0F;
        lastPlacedPitchDiff = 0.0F;
        replaying = false;
    }

    private void sendNoEvent(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        replaying = true;
        try {
            mc.getNetworkHandler().getConnection().send(packet);
        } finally {
            replaying = false;
        }
    }

    private void log(String message) {
        if (logging.getBoolean() && mc.player != null) {
            mc.player.sendMessage(Text.literal("[Disabler] " + message), false);
        }
    }
}
