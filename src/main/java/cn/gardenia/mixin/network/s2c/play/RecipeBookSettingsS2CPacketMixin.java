package cn.gardenia.mixin.network.s2c.play;

import net.minecraft.network.packet.s2c.play.RecipeBookSettingsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookSettingsS2CPacket.class)
public class RecipeBookSettingsS2CPacketMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(CallbackInfo ci) {
    }
}
