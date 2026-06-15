package cn.gardenia.mixin.client.render.entity;

import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void gardenia$applyServerRotation(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player) {
            return;
        }

        if (!ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return;
        }

        Vec2f rotation = ToolManager.INSTANCE.ROTATION.hasAnimationRotation()
                ? ToolManager.INSTANCE.ROTATION.getAnimationRotation()
                : ToolManager.INSTANCE.ROTATION.getServerRotation();
        state.bodyYaw = rotation.x;
        state.yawDegrees = 0.0F;
        state.pitch = rotation.y;
    }
}
