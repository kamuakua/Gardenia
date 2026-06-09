package cn.gardenia.client.module.misc;

import cn.gardenia.client.command.CommandManager;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class InterceptModule extends Module {
    private final Setting<Boolean> chat = new Setting<>("Chat", "Mirror disconnect reason to chat", true);
    private final Setting<Boolean> blockScreen = new Setting<>("Block Screen", "Block the disconnect screen", false);
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    public InterceptModule() {
        super("Intercept", "Captures disconnect reasons and mirrors them to chat", Category.MISC);
        addSetting(chat);
        addSetting(blockScreen);
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
        if (!event.isReceive() || !(event.getPacket() instanceof DisconnectS2CPacket packet)) {
            return;
        }

        Text reason = packet.reason();
        String text = reason == null ? "Disconnected" : reason.getString();
        if (text == null || text.isBlank()) {
            text = "Disconnected";
        }

        if (chat.getBoolean()) {
            CommandManager.msg("§c[Intercept] §f" + text);
        }

        if (blockScreen.getBoolean()) {
            event.cancel();
        }
    }
}
