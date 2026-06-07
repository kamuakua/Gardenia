package cn.gardenia.mixin.network.c2s.play;

import net.minecraft.network.packet.c2s.play.PlayerSessionC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerSessionC2SPacket.class)
public class PlayerSessionC2SPacketMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }
}
