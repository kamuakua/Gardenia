package cn.gardenia.client.event.events.network;

import cn.gardenia.client.event.Event;
import net.minecraft.network.packet.Packet;

public class GlobalPacketEvent extends Event {
    private final Type type;
    private Packet<?> packet;

    public GlobalPacketEvent(Type type, Packet<?> packet) {
        this.type = type;
        this.packet = packet;
    }

    public Type getType() {
        return type;
    }

    public boolean isReceive() {
        return type == Type.RECEIVE;
    }

    public boolean isSend() {
        return type == Type.SEND;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public enum Type {
        RECEIVE,
        SEND
    }
}
