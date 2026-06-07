package cn.gardenia.client.module.combat;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.attack.CriticalHitEvent;
import java.util.function.Consumer;

public class CriticalsModule extends Module {
    private final Setting<String> mode = new Setting<>("Mode", "Critical hit mode", "Packet", new String[]{"Packet", "Jump", "MiniJump"});
    private final Consumer<CriticalHitEvent> criticalListener;

    public CriticalsModule() {
        super("Criticals", "Always deal critical hits", Category.COMBAT);
        addSetting(mode);
        this.criticalListener = this::onCritical;
    }

    @Override
    public void onEnable() {
        subscribe(CriticalHitEvent.class, criticalListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(CriticalHitEvent.class, criticalListener);
    }

    private void onCritical(CriticalHitEvent event) {
        if (mc.player == null) return;
        if (mc.player.isTouchingWater() || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)) return;
        if (!mc.player.isOnGround()) return;

        event.setModifier(1.5f);

        String m = mode.getString();
        if (m.equals("Jump")) {
            mc.player.jump();
        } else if (m.equals("MiniJump")) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        }
    }
}