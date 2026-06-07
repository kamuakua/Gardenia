package cn.gardenia.mixin.network.s2c.play;

import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnterReconfigurationS2CPacket.class)
public class EnterReconfigurationS2CPacketMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(CallbackInfo ci) {
    }
}
