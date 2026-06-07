package cn.gardenia.client.tool.render;

import com.mojang.blaze3d.systems.RenderSystem;

public class GLTool {
    public void init() {}

    public void enableBlend() {
        RenderSystem.enableBlend();
    }

    public void disableBlend() {
        RenderSystem.disableBlend();
    }

    public void blendFunc(GLFunctions.SrcFactor src, GLFunctions.DstFactor dst) {
        RenderSystem.blendFunc(src.value, dst.value);
    }

    public void defaultBlend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public void enableDepth() {
        RenderSystem.enableDepthTest();
    }

    public void disableDepth() {
        RenderSystem.disableDepthTest();
    }

    public void depthFunc(GLFunctions.DepthFunc func) {
        RenderSystem.depthFunc(func.value);
    }

    public void enableCull() {
        RenderSystem.enableCull();
    }

    public void disableCull() {
        RenderSystem.disableCull();
    }

    public void enableScissor(int x, int y, int width, int height) {
        RenderSystem.enableScissor(x, y, width, height);
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }

    public void lineWidth(float width) {
        RenderSystem.lineWidth(width);
    }

    public void enablePolygonOffset() {
        RenderSystem.enablePolygonOffset();
    }

    public void disablePolygonOffset() {
        RenderSystem.disablePolygonOffset();
    }

    public void clearColor(float r, float g, float b, float a) {
        RenderSystem.clearColor(r, g, b, a);
    }

    public void clear(int mask) {
        RenderSystem.clear(mask);
    }

    public void viewport(int x, int y, int width, int height) {
        RenderSystem.viewport(x, y, width, height);
    }

    public void pushMatrix() {
        RenderSystem.getModelViewStack().pushMatrix();
    }

    public void popMatrix() {
        RenderSystem.getModelViewStack().popMatrix();
    }

    public void translate(float x, float y, float z) {
        RenderSystem.getModelViewStack().translate(x, y, z);
    }

    public void scale(float x, float y, float z) {
        RenderSystem.getModelViewStack().scale(x, y, z);
    }

    public void rotate(float angle, float x, float y, float z) {
        float rad = angle * 0.017453292f;
        RenderSystem.getModelViewStack().rotate(new org.joml.Quaternionf().rotationAxis(rad, x, y, z));
    }
}
