package cn.gardenia.client.event.events.network;

import cn.gardenia.client.event.Event;
import net.minecraft.network.packet.Packet;

public class PacketReceiveEvent extends Event {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() { return packet; }
}
