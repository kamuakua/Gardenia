package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.movement.PlayerMotionContext;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.movement.MovementEvent;
import cn.gardenia.client.event.events.movement.NewVelocityGameTickEvent;
import cn.gardenia.client.event.events.player.*;
import cn.gardenia.client.module.move.NoSlowModule;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    private PlayerMotionEvent gardenia$motionEvent;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerTickEvent(EventPhase.PRE));
        ToolManager.INSTANCE.ROTATION.tickServerRotation();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerTickEvent(EventPhase.POST));
    }

    @ModifyConstant(method = "tickMovement", constant = @Constant(floatValue = 0.2F))
    private float modifyUseItemSlowdown(float original) {
        return NoSlowModule.shouldPreventInputSlowdown() ? 1.0F : original;
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        EventBus.INSTANCE.post(new NewVelocityGameTickEvent());
    }

    @Inject(method = "swingHand", at = @At("HEAD"))
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerSwingHandEvent(hand == Hand.OFF_HAND));
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        MovementEvent event = new MovementEvent(type, movement);
        EventBus.INSTANCE.post(event);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        Vec2f rotation = ToolManager.INSTANCE.ROTATION.isServerRotationActive()
                ? ToolManager.INSTANCE.ROTATION.getServerRotation()
                : new Vec2f(player.getYaw(), player.getPitch());
        PlayerMotionEvent event = new PlayerMotionEvent(
                EventPhase.PRE,
                player.getX(),
                player.getY(),
                player.getZ(),
                rotation.x,
                rotation.y,
                player.isOnGround()
        );
        ToolManager.INSTANCE.ROTATION.onMotionPre(event.getYaw(), event.getPitch());
        EventBus.INSTANCE.post(event);
        gardenia$motionEvent = event;
        PlayerMotionContext.setActiveEvent(event);
        if (event.isCancelled()) {
            EventBus.INSTANCE.post(new PlayerMotionEvent(EventPhase.POST, event.getX(), event.getY(), event.getZ(), event.getYaw(), event.getPitch(), event.isOnGround()));
            gardenia$motionEvent = null;
            PlayerMotionContext.clear();
            ci.cancel();
        } else if (event.getYaw() != rotation.x || event.getPitch() != rotation.y) {
            ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(event.getYaw(), event.getPitch()), 180.0D);
        }
    }

    @Inject(
            method = "sendMovementPackets",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendSprintingPacket()V", shift = Shift.BEFORE)
    )
    private void onSendSprintingPacket(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        EventBus.INSTANCE.post(new PlayerSprintEvent(player.isSprinting()));
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo ci) {
        PlayerMotionEvent event = gardenia$motionEvent;
        if (event != null) {
            EventBus.INSTANCE.post(new PlayerMotionEvent(EventPhase.POST, event.getX(), event.getY(), event.getZ(), event.getYaw(), event.getPitch(), event.isOnGround()));
            gardenia$motionEvent = null;
            PlayerMotionContext.clear();
        }
    }
}
