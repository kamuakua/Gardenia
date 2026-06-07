package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.attack.AttackEntityEvent;
import cn.gardenia.client.event.events.block.BlockBreakEvent;
import cn.gardenia.client.event.events.block.BlockInteractEvent;
import cn.gardenia.client.event.events.block.BlockPlaceEvent;
import cn.gardenia.client.event.events.item.ItemUseEvent;
import cn.gardenia.client.event.events.item.PositionItemEvent;
import cn.gardenia.mixin.client.world.ClientWorldAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow
    private void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator) {
        throw new AssertionError();
    }

    @Shadow
    private ClientPlayNetworkHandler networkHandler;

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakEvent event = new BlockBreakEvent(pos, 0, direction);
        EventBus.INSTANCE.post(event);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEntityEvent event = new AttackEntityEvent(target, 0);
        EventBus.INSTANCE.post(event);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        BlockInteractEvent event = new BlockInteractEvent(hitResult.getBlockPos(), hitResult.getSide());
        EventBus.INSTANCE.post(event);
    }

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isEmpty()) {
            EventBus.INSTANCE.post(new ItemUseEvent(stack, player.getItemUseTime()));
        }
    }

    @Redirect(
            method = "interactItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V")
    )
    private void onInteractItemPacket(ClientPlayerInteractionManager instance, ClientWorld world, SequencedPacketCreator packetCreator) {
        PendingUpdateManager manager = ((ClientWorldAccessor) world).gardenia$getPendingUpdateManager();
        try (PendingUpdateManager pendingUpdateManager = manager.incrementSequence()) {
            Packet<?> packet = packetCreator.predict(pendingUpdateManager.getSequence());
            PositionItemEvent event = new PositionItemEvent(packet);
            EventBus.INSTANCE.post(event);
            if (!event.isCancelled() && event.getPacket() != null) {
                networkHandler.sendPacket((Packet<ServerPlayPacketListener>) event.getPacket());
            }
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakEvent event = new BlockBreakEvent(pos, -1, direction);
        EventBus.INSTANCE.post(event);
    }
}
