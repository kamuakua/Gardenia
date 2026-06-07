package cn.gardenia.mixin.client.item;

import cn.gardenia.client.tool.ToolManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public class ItemMixin {
    @Redirect(
            method = "raycast",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F")
    )
    private static float hookRayTraceYaw(PlayerEntity instance) {
        if (instance == ToolManager.INSTANCE.PLAYER.get() && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().x;
        }
        return instance.getYaw();
    }

    @Redirect(
            method = "raycast",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getPitch()F")
    )
    private static float hookRayTracePitch(PlayerEntity instance) {
        if (instance == ToolManager.INSTANCE.PLAYER.get() && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().y;
        }
        return instance.getPitch();
    }
}
