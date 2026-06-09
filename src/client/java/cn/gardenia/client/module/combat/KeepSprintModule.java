package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.attack.AttackSlowdownEvent;
import cn.gardenia.client.event.events.entity.EntityRemoveEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class KeepSprintModule extends Module {
    private final Setting<String> mode = new Setting<>("Mode", "Keep sprint mode", "Vanilla", new String[]{"Vanilla", "Prediction"});
    private final Setting<Boolean> cancelAtHurt = new Setting<>("CancelAtHurt", "Do not keep sprint immediately after taking damage", false);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<AttackSlowdownEvent> attackSlowdownListener = this::onAttackSlowdown;
    private final Consumer<EntityRemoveEvent> attackListener = this::onAttack;

    private int hurtTicksRemaining;
    private int lastHurtTime;

    public KeepSprintModule() {
        super("KeepSprint", "Keep sprint on attack", Category.COMBAT);
        addSetting(mode);
        addSetting(cancelAtHurt);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(AttackSlowdownEvent.class, attackSlowdownListener);
        subscribe(EntityRemoveEvent.class, attackListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(AttackSlowdownEvent.class, attackSlowdownListener);
        unsubscribe(EntityRemoveEvent.class, attackListener);
        hurtTicksRemaining = 0;
        lastHurtTime = 0;
    }

    public String getSuffix() {
        return mode.getString();
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null) {
            return;
        }

        if (mc.player.hurtTime > 0 && lastHurtTime == 0) {
            hurtTicksRemaining = 2;
        }
        lastHurtTime = mc.player.hurtTime;
        if (hurtTicksRemaining > 0) {
            hurtTicksRemaining--;
        }
    }

    private void onAttackSlowdown(AttackSlowdownEvent event) {
        if (mc.player == null) {
            return;
        }

        if ("Vanilla".equals(mode.getString())) {
            if (!cancelAtHurt.getBoolean() || hurtTicksRemaining <= 0) {
                event.setReduce(1.0D);
                event.setSprint(true);
            }
            return;
        }

        if ("Prediction".equals(mode.getString())) {
            event.setSprint(true);
            mc.player.setSprinting(true);
        }
    }

    private void onAttack(EntityRemoveEvent event) {
        if (event.isDead() || mc.player == null || mc.getNetworkHandler() == null || !"Vanilla".equals(mode.getString())) {
            return;
        }

        int attackSlot = mc.player.getInventory().selectedSlot;
        int otherSlot;
        do {
            otherSlot = ThreadLocalRandom.current().nextInt(0, 9);
        } while (otherSlot == attackSlot);

        mc.player.getInventory().selectedSlot = otherSlot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(otherSlot));
        mc.player.getInventory().selectedSlot = attackSlot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(attackSlot));
    }
}
