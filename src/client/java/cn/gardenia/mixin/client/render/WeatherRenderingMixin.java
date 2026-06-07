package cn.gardenia.mixin.client.render;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderWeatherEvent;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {
    @Inject(method = "renderPrecipitation", at = @At("HEAD"))
    private void onRenderPrecipitation(World world, VertexConsumerProvider vertexConsumers, int ticks, float tickDelta, Vec3d pos, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderWeatherEvent(tickDelta));
    }
}
