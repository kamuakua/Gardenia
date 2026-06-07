package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.util.math.MatrixStack;

public class RenderWorldEvent extends Event {
    private final float tickDelta;
    private final MatrixStack matrices;

    public RenderWorldEvent(float tickDelta, MatrixStack matrices) {
        this.tickDelta = tickDelta;
        this.matrices = matrices;
    }

    public float getTickDelta() { return tickDelta; }
    public MatrixStack getMatrices() { return matrices; }
}
