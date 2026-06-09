package cn.gardenia.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void gardenia$setRightClickDelay(int delay);

    @Accessor("attackCooldown")
    void gardenia$setMissTime(int ticks);
}
