package cn.gardenia.mixin.network.s2c.config;

import net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicRegistriesS2CPacket.class)
public class DynamicRegistriesS2CPacketMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(CallbackInfo ci) {
    }
}
