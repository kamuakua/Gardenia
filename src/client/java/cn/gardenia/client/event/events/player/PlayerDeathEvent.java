package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;
import net.minecraft.entity.damage.DamageSource;

public class PlayerDeathEvent extends Event {
    private final DamageSource damageSource;

    public PlayerDeathEvent(DamageSource damageSource) {
        this.damageSource = damageSource;
    }

    public DamageSource getDamageSource() { return damageSource; }
}
