package cn.gardenia.mixin.network.c2s.play;

import net.minecraft.network.packet.c2s.play.AcknowledgeReconfigurationC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AcknowledgeReconfigurationC2SPacket.class)
public class AcknowledgeReconfigurationC2SPacketMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
    }
}
