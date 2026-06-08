package cn.gardenia.mixin.client.input;

import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.module.combat.CriticalsModule;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        this.playerInput = Velocity.handleMoveInput(this.playerInput);
        this.playerInput = CriticalsModule.handleMoveInput(this.playerInput);
        if (ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            Vec2f fixed = ToolManager.INSTANCE.ROTATION.fixMovement(
                    this.movementForward,
                    this.movementSideways,
                    ToolManager.INSTANCE.ROTATION.getServerRotation().x
            );
            this.movementSideways = fixed.x;
            this.movementForward = fixed.y;
        }
    }
}
