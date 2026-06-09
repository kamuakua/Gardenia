package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.mixin.client.MinecraftClientAccessor;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class AutoClickerModule extends Module {
    private final Setting<Integer> minCps = new Setting<>("Min CPS", "Minimum clicks per second", 12, 1, 20);
    private final Setting<Integer> maxCps = new Setting<>("Max CPS", "Maximum clicks per second", 18, 1, 20);
    private final Setting<Boolean> itemCheck = new Setting<>("Item Check", "Only click with swords and axes", false);
    private final Consumer<PlayerTickEvent> tickListener = this::onTickEvent;
    private long lastAttackTime;

    public AutoClickerModule() {
        super("AutoClicker", "Automatically clicks for you", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(itemCheck);
    }

    @Override
    public void onEnable() {
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    private void onTickEvent(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.options == null || !mc.options.attackKey.isPressed()) {
            return;
        }

        Item item = mc.player.getMainHandStack().getItem();
        if (itemCheck.getBoolean() && !(item instanceof SwordItem) && !(item instanceof AxeItem)) {
            return;
        }

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return;
        }

        long now = System.currentTimeMillis();
        double baseDelay = 1000.0D / randomCps();
        long delay = (long) (baseDelay + (Math.random() - 0.5D) * baseDelay * 0.4D);
        if (now - lastAttackTime < delay) {
            return;
        }

        ((MinecraftClientAccessor) mc).gardenia$setMissTime(0);
        if (mc.interactionManager != null && mc.crosshairTarget instanceof EntityHitResult hit) {
            mc.interactionManager.attackEntity(mc.player, hit.getEntity());
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        lastAttackTime = now;
    }

    private int randomCps() {
        int min = Math.min(minCps.getInt(), maxCps.getInt());
        int max = Math.max(minCps.getInt(), maxCps.getInt());
        return min >= max ? Math.max(1, min) : ThreadLocalRandom.current().nextInt(Math.max(1, min), max + 1);
    }
}
