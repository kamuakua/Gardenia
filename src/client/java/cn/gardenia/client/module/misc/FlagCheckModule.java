package cn.gardenia.client.module.misc;

import cn.gardenia.client.command.CommandManager;
import cn.gardenia.client.event.events.network.PacketReceiveEvent;
import cn.gardenia.client.gui.notification.Notification;
import cn.gardenia.client.gui.notification.NotificationManager;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.function.Consumer;

public class FlagCheckModule extends Module {
    private final Setting<Boolean> notification = new Setting<>("Notification", "Show flag notifications", false);
    private final Setting<Boolean> chat = new Setting<>("Chat", "Print flag alerts to chat", false);
    private final Consumer<PacketReceiveEvent> packetListener = this::onPacketReceive;

    private int flagCount;
    private float lastYaw;
    private float lastPitch;

    public FlagCheckModule() {
        super("FlagCheck", "Check flag and alert", Category.MISC);
        addSetting(notification);
        addSetting(chat);
    }

    @Override
    public void onEnable() {
        flagCount = 0;
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
        subscribe(PacketReceiveEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PacketReceiveEvent.class, packetListener);
        flagCount = 0;
    }

    private void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null || mc.player.age <= 25) {
            return;
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
            flagCount++;

            float serverYaw = packet.change().yaw();
            float serverPitch = packet.change().pitch();
            float deltaYaw = calculateAngleDelta(serverYaw, lastYaw);
            float deltaPitch = calculateAngleDelta(serverPitch, lastPitch);

            if (deltaYaw >= 90.0F || deltaPitch >= 90.0F) {
                alert("强制旋转", String.format("(%.1f° | %.1f°)", deltaYaw, deltaPitch));
            } else {
                alert("回弹", "");
            }

            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    private float calculateAngleDelta(float newAngle, float oldAngle) {
        float delta = newAngle - oldAngle;
        if (delta > 180.0F) {
            delta -= 360.0F;
        }
        if (delta < -180.0F) {
            delta += 360.0F;
        }
        return Math.abs(delta);
    }

    private void alert(String reason, String extra) {
        String message = extra.isEmpty()
                ? String.format("§f服务器检测到 §c%s§f，总计 §c%d§f 次。", reason, flagCount)
                : String.format("§f服务器检测到 §c%s§f %s，总计 §c%d§f 次。", reason, extra, flagCount);

        if (chat.getBoolean()) {
            CommandManager.msg("[FlagCheck] " + message);
        }

        if (notification.getBoolean()) {
            NotificationManager.INSTANCE.show(new Notification("FlagCheck", message, "notification_off", 5000));
        }
    }
}
