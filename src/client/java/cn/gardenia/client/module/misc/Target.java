package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WaterAnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Target extends Module {
    private final Setting<Boolean> player = new Setting<>("Player", "Target players", true);
    private final Setting<Boolean> invisibles = new Setting<>("Invisibles", "Target invisible living entities", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", "Target animals", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", "Target mobs", true);
    private final Setting<Boolean> villager = new Setting<>("Villager", "Target villagers", true);

    public Target() {
        super("Target", "Target selector", Category.MISC);
        addSetting(player);
        addSetting(invisibles);
        addSetting(animals);
        addSetting(mobs);
        addSetting(villager);
    }

    @Override
    public void onEnable() {
        setEnabled(false);
    }

    public boolean isTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player) return false;
        if (entity instanceof PlayerEntity && entity.isSpectator()) return false;
        if (!entity.isAlive()) return false;

        if (entity.isInvisible() && !invisibles.getBoolean()) {
            return false;
        }

        if (entity instanceof PlayerEntity) {
            return player.getBoolean();
        }

        if (entity instanceof VillagerEntity) {
            return villager.getBoolean();
        }

        if (entity instanceof Monster || entity instanceof SlimeEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart) {
            return mobs.getBoolean();
        }

        if (entity instanceof AnimalEntity || entity instanceof AmbientEntity || entity instanceof WaterAnimalEntity || entity instanceof AbstractHorseEntity) {
            return animals.getBoolean();
        }

        return false;
    }
}
