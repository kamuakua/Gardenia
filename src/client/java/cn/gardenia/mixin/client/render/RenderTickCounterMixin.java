package cn.gardenia.mixin.client.render;

import cn.gardenia.client.tool.player.GardeniaTickTimer;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterMixin {
    @Shadow
    private float lastFrameDuration;

    @Shadow
    private float tickDelta;

    @Shadow
    private long prevTimeMillis;

    @Shadow
    @Final
    private float tickTime;

    @Shadow
    @Final
    private FloatUnaryOperator targetMillisPerTick;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
    private void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        float multiplier = GardeniaTickTimer.multiplier();
        if (multiplier == 1.0F) {
            return;
        }

        this.lastFrameDuration = (float) (timeMillis - this.prevTimeMillis)
                / this.targetMillisPerTick.apply(this.tickTime)
                * multiplier;
        this.prevTimeMillis = timeMillis;
        this.tickDelta += this.lastFrameDuration;
        int ticks = (int) this.tickDelta;
        this.tickDelta -= (float) ticks;
        cir.setReturnValue(ticks);
    }
}
