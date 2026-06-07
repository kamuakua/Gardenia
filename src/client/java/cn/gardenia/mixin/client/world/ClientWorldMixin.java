package cn.gardenia.mixin.client.world;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.world.WorldLoadEvent;
import cn.gardenia.client.event.events.world.WorldUnloadEvent;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
    }
}
