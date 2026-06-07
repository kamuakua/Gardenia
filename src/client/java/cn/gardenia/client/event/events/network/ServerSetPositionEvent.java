package cn.gardenia.client.event.events.network;

import cn.gardenia.client.event.Event;
import net.minecraft.network.packet.Packet;

public class ServerSetPositionEvent extends Event {
    private Packet<?> packet;

    public ServerSetPositionEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
