package cn.gardenia.mixin.client.render.entity;

import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    // 渲染用的平滑朝向：跟当前实际服务端旋转通过低通滤波收敛，避免每 tick 硬切。
    // 这只影响第三人称模型外观，不影响发给服务器的旋转，所以再怎么平滑都不会触发反作弊。
    private static float gardenia$renderYaw = Float.NaN;
    private static float gardenia$renderPitch = Float.NaN;

    // 每帧靠近目标的比例。值越小越软，越大越跟手。0.18 在 60fps 下约 0.2 秒收敛 90%，看起来是自然转身。
    private static final float SMOOTH_FACTOR = 0.18F;

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void gardenia$applyServerRotation(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player) {
            return;
        }

        boolean active = ToolManager.INSTANCE.ROTATION.isServerRotationActive();
        // 目标朝向：active 时是服务端旋转（放块方向），否则是玩家真实朝向（让模型平滑转回）。
        float targetYaw;
        float targetPitch;
        if (active) {
            Vec2f rotation = ToolManager.INSTANCE.ROTATION.getServerRotation();
            targetYaw = rotation.x;
            targetPitch = rotation.y;
        } else {
            targetYaw = state.bodyYaw;
            targetPitch = state.pitch;
        }

        // 首次或断流后用目标值初始化，避免冷启动从 0 一路转过来。
        if (Float.isNaN(gardenia$renderYaw) || Float.isNaN(gardenia$renderPitch)) {
            gardenia$renderYaw = targetYaw;
            gardenia$renderPitch = targetPitch;
        }

        // 低通滤波：每帧朝目标走一小段。yaw 用 wrapDegrees 处理 360° 跨界。
        float yawDelta = MathHelper.wrapDegrees(targetYaw - gardenia$renderYaw);
        gardenia$renderYaw += yawDelta * SMOOTH_FACTOR;
        gardenia$renderPitch += (targetPitch - gardenia$renderPitch) * SMOOTH_FACTOR;

        // active 完全收敛回真实朝向后，让 vanilla 接管（避免一直 hijack 渲染）。
        if (!active) {
            float remainYaw = Math.abs(MathHelper.wrapDegrees(state.bodyYaw - gardenia$renderYaw));
            float remainPitch = Math.abs(state.pitch - gardenia$renderPitch);
            if (remainYaw < 0.5F && remainPitch < 0.5F) {
                return;
            }
        }

        // 写回渲染状态：身体与头同向（整体转身），pitch 跟随。
        state.bodyYaw = gardenia$renderYaw;
        state.yawDegrees = 0.0F;
        state.pitch = gardenia$renderPitch;
    }
}
