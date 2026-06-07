package cn.gardenia.mixin.network.c2s.config;

import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectKnownPacksC2SPacket.class)
public class SelectKnownPacksC2SPacketMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }
}
