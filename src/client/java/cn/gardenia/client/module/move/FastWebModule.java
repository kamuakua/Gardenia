package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.StuckInBlockEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

public class FastWebModule extends Module {
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<StuckInBlockEvent> stuckListener = this::onStuckInBlock;

    private int lastWebTick;
    private int webCount;

    public FastWebModule() {
        super("FastWeb", "Fast cobweb movement", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        lastWebTick = 0;
        webCount = 0;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(StuckInBlockEvent.class, stuckListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(StuckInBlockEvent.class, stuckListener);
        webCount = 0;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null) {
            return;
        }
        if (lastWebTick < mc.player.age) {
            webCount = 0;
        }
        if (webCount > 1) {
            mc.player.setSprinting(false);
        }
    }

    private void onStuckInBlock(StuckInBlockEvent event) {
        if (mc.player == null || !event.getState().isOf(Blocks.COBWEB)) {
            return;
        }
        lastWebTick = mc.player.age;
        webCount++;
        if (webCount > 5) {
            event.setSpeedMultiplier(new Vec3d(0.88D, 1.88D, 0.88D));
        }
    }
}
