package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class LongJumpModule extends Module {
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    private boolean notMoving;
    private boolean firstEnableTick;
    private int rotateTick;
    private int lastSlot = -1;
    private boolean delayed;
    private boolean shouldDisableAndRelease;
    private boolean usingItem;
    private boolean mouse4Pressed;
    private boolean mouse5Pressed;
    private int usedFireballCount;
    private int receivedKnockbacks;
    private int initialFireballCount;
    private int releasedKnockbacks;
    private boolean releasingPackets;

    public LongJumpModule() {
        super("LongJump", "Uses fire charges for packet-delayed long jumps", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        releaseAll();
        rotateTick = 0;
        firstEnableTick = true;
        lastSlot = -1;
        notMoving = false;
        delayed = false;
        usingItem = false;
        shouldDisableAndRelease = false;
        mouse4Pressed = false;
        mouse5Pressed = false;
        usedFireballCount = 0;
        receivedKnockbacks = 0;
        initialFireballCount = 0;
        releasedKnockbacks = 0;
        knockbackPositions.clear();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        releaseAll();
        restoreSlot();
        if (mc.options != null) {
            mc.options.useKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
        ToolManager.INSTANCE.ROTATION.clearServerRotation();
        usingItem = false;
        shouldDisableAndRelease = false;
        mouse4Pressed = false;
        mouse5Pressed = false;
        usedFireballCount = 0;
        receivedKnockbacks = 0;
        initialFireballCount = 0;
        releasedKnockbacks = 0;
        knockbackPositions.clear();
        packets.clear();
        releasingPackets = false;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        if (shouldDisableAndRelease) {
            setEnabled(false);
            return;
        }

        if (firstEnableTick) {
            notMoving = !isMoving();
            firstEnableTick = false;
        }

        boolean mouse4 = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS;
        if (mouse4 && !mouse4Pressed) {
            mouse4Pressed = true;
            startFireballUse();
        } else if (!mouse4) {
            mouse4Pressed = false;
        }

        boolean mouse5 = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;
        if (mouse5 && !mouse5Pressed) {
            mouse5Pressed = true;
            releaseNextKnockback();
        } else if (!mouse5) {
            mouse5Pressed = false;
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.isPre()) {
            handlePreMotion();
        } else if (event.isPost() && usingItem) {
            handlePostMotion();
        }
    }

    private void handlePreMotion() {
        if (rotateTick <= 0) {
            return;
        }

        if (rotateTick == 1) {
            usedFireballCount++;
            mc.options.jumpKey.setPressed(true);
            float yaw = notMoving ? mc.player.getYaw() : mc.player.getYaw() - 180.0F;
            float pitch = notMoving ? 90.0F : 88.0F;
            ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(yaw, pitch), 360.0D);
        }

        if (rotateTick >= 2) {
            rotateTick = 0;
            int fireballSlot = setupFireballSlot();
            if (fireballSlot == -1) {
                setEnabled(false);
                return;
            }
            selectSlot(fireballSlot);
            initialFireballCount = getFireballCount();
            mc.options.useKey.setPressed(true);
            usingItem = true;
            return;
        }

        rotateTick++;
    }

    private void handlePostMotion() {
        int currentFireballCount = getFireballCount();
        if (currentFireballCount < initialFireballCount || getFireballSlot() == -1) {
            mc.options.useKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            ToolManager.INSTANCE.ROTATION.clearServerRotation();
            usingItem = false;
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || releasingPackets || !event.isReceive()) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (delayed) {
            if (packet instanceof PlayerPositionLookS2CPacket) {
                shouldDisableAndRelease = true;
                event.cancel();
                return;
            }

            if (packet instanceof EntityVelocityUpdateS2CPacket velocity && velocity.getEntityId() == mc.player.getId()) {
                receivedKnockbacks++;
                knockbackPositions.add(packets.size());
            }

            event.cancel();
            packets.offer(packet);
            return;
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket velocity
                && velocity.getEntityId() == mc.player.getId()
                && usedFireballCount > 0) {
            receivedKnockbacks++;
            knockbackPositions.add(packets.size());
            event.cancel();
            packets.offer(packet);
            delayed = true;
        }
    }

    private void startFireballUse() {
        if (usingItem || rotateTick != 0 || mc.player == null) {
            return;
        }

        int fireballSlot = setupFireballSlot();
        if (fireballSlot == -1) {
            return;
        }

        lastSlot = mc.player.getInventory().selectedSlot;
        selectSlot(fireballSlot);
        rotateTick = 1;
    }

    private void releaseNextKnockback() {
        if (!delayed) {
            setEnabled(false);
            return;
        }

        if (releasedKnockbacks < receivedKnockbacks) {
            releaseToKnockback(releasedKnockbacks);
            releasedKnockbacks++;
        }

        if (releasedKnockbacks >= receivedKnockbacks) {
            delayed = false;
            setEnabled(false);
        }
    }

    private int setupFireballSlot() {
        int fireballSlot = getFireballSlot();
        if (fireballSlot == -1) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("No fire charge"), false);
            }
            setEnabled(false);
        }
        return fireballSlot;
    }

    private int getFireballSlot() {
        if (mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.FIRE_CHARGE)) {
                return i;
            }
        }
        return -1;
    }

    private int getFireballCount() {
        if (mc.player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.FIRE_CHARGE)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void selectSlot(int slot) {
        if (mc.player != null && slot >= 0 && slot < 9) {
            if (mc.player.getInventory().selectedSlot == slot) {
                return;
            }
            mc.player.getInventory().selectedSlot = slot;
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }
    }

    private void restoreSlot() {
        if (lastSlot != -1 && mc.player != null) {
            selectSlot(lastSlot);
            lastSlot = -1;
        }
    }

    private void releaseAll() {
        Packet<?> packet;
        while ((packet = packets.poll()) != null) {
            handleInbound(packet);
        }
    }

    private void releaseToKnockback(int knockbackIndex) {
        if (knockbackIndex >= knockbackPositions.size()) {
            return;
        }

        int targetPosition = knockbackPositions.get(knockbackIndex);
        int releasedCount = 0;
        while (!packets.isEmpty() && releasedCount <= targetPosition) {
            handleInbound(packets.poll());
            releasedCount++;
        }

        for (int i = knockbackIndex + 1; i < knockbackPositions.size(); i++) {
            knockbackPositions.set(i, knockbackPositions.get(i) - (targetPosition + 1));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleInbound(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        releasingPackets = true;
        try {
            ((Packet) packet).apply((PacketListener) mc.getNetworkHandler());
        } finally {
            releasingPackets = false;
        }
    }

    private boolean isMoving() {
        return mc.player != null
                && (Math.abs(mc.player.input.movementForward) > 0.001F
                || Math.abs(mc.player.input.movementSideways) > 0.001F);
    }
}
