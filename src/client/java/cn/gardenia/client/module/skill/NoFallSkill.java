package cn.gardenia.client.module.skill;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Category;

public class NoFallSkill extends Module {
    public NoFallSkill() {
        super("NoFall", "Negates fall damage", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.fallDistance > 2.0f) {
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    0.0,
                    mc.player.getVelocity().z
            );
            mc.player.fallDistance = 0.0f;
        }
    }
}
