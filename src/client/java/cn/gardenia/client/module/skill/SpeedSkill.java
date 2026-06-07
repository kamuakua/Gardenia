package cn.gardenia.client.module.skill;

import cn.gardenia.client.module.Skill;

public class SpeedSkill extends Skill {
    public SpeedSkill() {
        super("Speed", "Increases movement speed temporarily", 40);
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        mc.player.setVelocity(
                mc.player.getVelocity().x * 1.5,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z * 1.5
        );
    }
}
