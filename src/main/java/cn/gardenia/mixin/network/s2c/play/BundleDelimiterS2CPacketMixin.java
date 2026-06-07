package cn.gardenia.mixin.network.s2c.play;

import net.minecraft.network.packet.s2c.play.BundleDelimiterS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BundleDelimiterS2CPacket.class)
public class BundleDelimiterS2CPacketMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }
}
