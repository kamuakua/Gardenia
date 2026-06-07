package cn.gardenia.mixin.client.render;

import cn.gardenia.client.module.render.SwordBlockModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    private static boolean isBlockingWithSword() {
        SwordBlockModule module = SwordBlockModule.getInstance();
        if (module == null || !module.isBlocking()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof SwordItem;
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void onApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (isBlockingWithSword()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwordBlockModule module = SwordBlockModule.getInstance();
        if (module == null || !module.isBlocking()) return;
        if (hand != Hand.MAIN_HAND) return;
        if (!(item.getItem() instanceof SwordItem)) return;

        boolean rightHand = player.getMainArm() == Arm.RIGHT;
        if (hand == Hand.MAIN_HAND) {
            if (rightHand) {
                matrices.translate(-0.4F, 0.5F, -0.35F);
                matrices.multiply(
                    new org.joml.Quaternionf().rotateAxis(
                        (float) Math.toRadians(-90.0F),
                        new org.joml.Vector3f(1.0F, 0.0F, 0.0F)
                    )
                );
                matrices.multiply(
                    new org.joml.Quaternionf().rotateAxis(
                        (float) Math.toRadians(55.0F),
                        new org.joml.Vector3f(0.0F, 0.0F, 1.0F)
                    )
                );

                if (swingProgress > 0.0F) {
                    float t = swingProgress;
                    float angle = (float) Math.toRadians(25.0F) * (1.0F - t) * t * 4.0F;
                    matrices.multiply(
                        new org.joml.Quaternionf().rotateAxis(
                            angle,
                            new org.joml.Vector3f(1.0F, 0.0F, 0.0F)
                        )
                    );
                }
            }
        }
    }
}
