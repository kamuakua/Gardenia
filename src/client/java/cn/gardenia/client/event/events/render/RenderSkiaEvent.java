package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import io.github.humbleui.skija.Canvas;

public class RenderSkiaEvent extends Event {
    private final Canvas canvas;
    private final float tickDelta;
    private final int width;
    private final int height;
    private final float scaleFactor;

    public RenderSkiaEvent(Canvas canvas, float tickDelta, int width, int height, float scaleFactor) {
        this.canvas = canvas;
        this.tickDelta = tickDelta;
        this.width = width;
        this.height = height;
        this.scaleFactor = scaleFactor;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }
}
