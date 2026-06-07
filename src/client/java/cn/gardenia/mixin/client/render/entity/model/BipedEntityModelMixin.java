package cn.gardenia.mixin.client.render.entity.model;

import cn.gardenia.client.module.render.SwordBlockModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void onSetAngles(BipedEntityRenderState state, CallbackInfo ci) {
        SwordBlockModule module = SwordBlockModule.getInstance();
        if (module == null || !module.isBlocking()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean sneaking = state.isInSneakingPose;

        rightArm.pitch = (float) Math.toRadians(-100.0F);
        rightArm.yaw = (float) Math.toRadians(20.0F);
        rightArm.roll = (float) Math.toRadians(0.0F);

        leftArm.pitch = (float) Math.toRadians(-40.0F);
        leftArm.yaw = (float) Math.toRadians(-10.0F);
        leftArm.roll = (float) Math.toRadians(0.0F);

        if (sneaking) {
            rightArm.pitch += (float) Math.toRadians(-25.0F);
            leftArm.pitch += (float) Math.toRadians(-25.0F);
        }
    }
}
