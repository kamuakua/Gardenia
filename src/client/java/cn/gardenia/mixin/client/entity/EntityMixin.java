package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.player.PlayerSneakEvent;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "setSneaking", at = @At("HEAD"))
    private void onSetSneaking(boolean sneaking, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerSneakEvent(sneaking));
    }

    @Inject(method = "getRotationVec", at = @At("HEAD"), cancellable = true)
    private void onGetRotationVec(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        Entity entity = (Entity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.player && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            Vec2f rotation = ToolManager.INSTANCE.ROTATION.getServerRotation();
            cir.setReturnValue(Vec3d.fromPolar(rotation.y, rotation.x));
        }
    }

    @ModifyArg(
            method = "updateVelocity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"),
            index = 2
    )
    private float modifyMovementYaw(float yaw) {
        Entity entity = (Entity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.player && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().x;
        }
        return yaw;
    }
}
