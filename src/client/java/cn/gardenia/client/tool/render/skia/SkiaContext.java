package cn.gardenia.client.tool.render.skia;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;

import java.util.function.Consumer;

public final class SkiaContext {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static DirectContext context;
    private static Surface surface;
    private static BackendRenderTarget renderTarget;
    private static int surfaceWidth = -1;
    private static int surfaceHeight = -1;

    private SkiaContext() {
    }

    public static boolean isReady() {
        return context != null && surface != null;
    }

    public static DirectContext getContext() {
        ensureSurface();
        return context;
    }

    public static Canvas getCanvas() {
        ensureSurface();
        return surface == null ? null : surface.getCanvas();
    }

    public static void draw(Consumer<Canvas> drawingLogic) {
        ensureSurface();
        if (context == null || surface == null || drawingLogic == null) {
            return;
        }

        RenderSystem.pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        RenderSystem.pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        context.resetGLAll();

        drawingLogic.accept(surface.getCanvas());
        context.flush();
        restoreGlState();
    }

    public static void ensureSurface() {
        if (mc.getFramebuffer() == null || mc.getWindow() == null) {
            return;
        }

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (surface != null && width == surfaceWidth && height == surfaceHeight) {
            return;
        }

        createSurface(width, height);
    }

    public static void createSurface(int width, int height) {
        if (width <= 0 || height <= 0 || mc.getFramebuffer() == null) {
            return;
        }

        if (context == null) {
            context = DirectContext.makeGL();
        }

        closeSurface();

        renderTarget = BackendRenderTarget.makeGL(
                width,
                height,
                0,
                8,
                mc.getFramebuffer().fbo,
                GL11.GL_RGBA8
        );
        surface = Surface.wrapBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.getSRGB()
        );
        surfaceWidth = width;
        surfaceHeight = height;
    }

    public static void close() {
        closeSurface();
        if (context != null) {
            context.close();
            context = null;
        }
    }

    private static void closeSurface() {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (renderTarget != null) {
            renderTarget.close();
            renderTarget = null;
        }
        surfaceWidth = -1;
        surfaceHeight = -1;
    }

    private static void restoreGlState() {
        GL33.glBindSampler(0, 0);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_BLEND);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendEquation(GL33.GL_FUNC_ADD);
        GL33.glBlendEquation(GL33.GL_FUNC_ADD);
        RenderSystem.colorMask(true, true, true, true);
        GL11.glColorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        GL11.glDepthMask(true);
        RenderSystem.disableScissor();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.disableDepthTest();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.disableCull();
    }
}
