package cn.gardenia.mixin.network.s2c.common;

import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTransferS2CPacket.class)
public class ServerTransferS2CPacketMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(CallbackInfo ci) {
    }
}
