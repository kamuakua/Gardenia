package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.Entity;

public class PlayerPushEvent extends Event {
    private final Entity pusher;
    private double amount;

    public PlayerPushEvent(Entity pusher, double amount) {
        this.pusher = pusher;
        this.amount = amount;
    }

    public Entity getPusher() { return pusher; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
