package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.network.packet.Packet;

public class PositionItemEvent extends Event {
    private Packet<?> packet;

    public PositionItemEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
