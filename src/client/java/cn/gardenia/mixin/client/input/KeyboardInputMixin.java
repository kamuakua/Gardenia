package cn.gardenia.mixin.client.input;

import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.module.combat.GrimveloModule;
import cn.gardenia.client.module.combat.NewVelocityModule;
import cn.gardenia.client.module.combat.CriticalsModule;
import cn.gardenia.client.module.combat.TestCriticalModule;
import cn.gardenia.client.module.misc.NewInvManagerModule;
import cn.gardenia.client.module.move.GrimLowHopModule;
import cn.gardenia.client.module.move.GrimSpeedModule;
import cn.gardenia.client.module.move.ScaffoldModule;
import cn.gardenia.client.module.move.StuckModule;
import cn.gardenia.client.module.move.TargetStrafeModule;
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
        this.playerInput = GrimveloModule.handleMoveInput(this.playerInput);
        this.playerInput = NewVelocityModule.handleMoveInput(this.playerInput);
        this.playerInput = CriticalsModule.handleMoveInput(this.playerInput);
        this.playerInput = TestCriticalModule.handleMoveInput(this.playerInput);
        this.playerInput = NewInvManagerModule.handleMoveInput(this.playerInput);
        this.playerInput = GrimLowHopModule.handleMoveInput(this.playerInput);
        this.playerInput = GrimSpeedModule.handleMoveInput(this.playerInput);
        this.playerInput = ScaffoldModule.handleMoveInput(this.playerInput);
        this.playerInput = TargetStrafeModule.handleMoveInput(this.playerInput);
        this.playerInput = StuckModule.handleMoveInput(this.playerInput);
        syncMovementFromPlayerInput();
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

    private void syncMovementFromPlayerInput() {
        this.movementForward = movementMultiplier(this.playerInput.forward(), this.playerInput.backward());
        this.movementSideways = movementMultiplier(this.playerInput.left(), this.playerInput.right());
        if (this.playerInput.sneak()) {
            this.movementForward *= 0.3F;
            this.movementSideways *= 0.3F;
        }
    }

    private float movementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
