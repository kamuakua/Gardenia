package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.player.PlayerJumpEvent;
import cn.gardenia.client.event.events.player.PlayerSprintEvent;
import cn.gardenia.client.event.events.player.PlayerTravelEvent;
import cn.gardenia.client.module.FullBrightModule;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerJumpEvent(0.42f));
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerTravelEvent event = new PlayerTravelEvent(
            (float) movementInput.x,
            (float) movementInput.z,
            (float) movementInput.y
        );
        EventBus.INSTANCE.post(event);
    }

    @Inject(method = "setSprinting", at = @At("HEAD"))
    private void onSetSprinting(boolean sprinting, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerSprintEvent(sprinting));
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        FullBrightModule fullBright = ModuleManager.INSTANCE.getByClass(FullBrightModule.class);
        if (entity == MinecraftClient.getInstance().player && effect == StatusEffects.NIGHT_VISION && fullBright != null && fullBright.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = "jump",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 0)
    )
    private float modifyJumpYaw(LivingEntity entity) {
        if (entity == MinecraftClient.getInstance().player && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().x;
        }
        return entity.getYaw();
    }

    @Redirect(
            method = "calcGlidingVelocity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F")
    )
    private float modifyFallFlyingPitch(LivingEntity entity) {
        if (entity == MinecraftClient.getInstance().player && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().y;
        }
        return entity.getPitch();
    }
}
