package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.util.Hand;

import java.util.function.Consumer;

public class AntiFireballModule extends Module {
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    public AntiFireballModule() {
        super("AntiFireball", "Prevents fireballs from damaging you", Category.MISC);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractFireballEntity && mc.player.distanceTo(entity) < 6.0F) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }
}
