package cn.gardenia.client.gui.nanovg;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class NanoVGManager {
    public static final NanoVGManager INSTANCE = new NanoVGManager();

    private long ctx = 0;
    private final Map<String, Integer> fonts = new HashMap<>();
    private final Map<String, Integer> images = new HashMap<>();
    private boolean initialized = false;
    private boolean frameActive = false;

    private NanoVGManager() {}

    public void init() {
        if (initialized) return;
        ctx = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (ctx == 0) {
            throw new RuntimeException("Failed to create NanoVG context");
        }
        loadFonts();
        initialized = true;
    }

    public void shutdown() {
        if (ctx != 0) {
            for (int img : images.values()) {
                NanoVG.nvgDeleteImage(ctx, img);
            }
            NanoVGGL3.nvgDelete(ctx);
            ctx = 0;
        }
        fonts.clear();
        images.clear();
        initialized = false;
    }

    public long getContext() {
        return ctx;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void loadFonts() {
        String basePath = "gardenia/fonts/";
        loadFont("regular", basePath + "Greycliff-CF-Regular.otf");
        loadFont("medium", basePath + "Greycliff-CF-Medium.otf");
        loadFont("bold", basePath + "Greycliff-CF-Bold.otf");
        loadFont("inter", basePath + "Inter_semibold.ttf");
        loadFont("axiforma", basePath + "axiforma_regular.ttf");
        loadFont("axiforma-bold", basePath + "axiforma_bold.ttf");
        loadFont("manrope", basePath + "manrope_medium.ttf");
        loadFont("manrope-bold", basePath + "manrope_bold.ttf");
        loadFont("icons", basePath + "feather-icon.ttf");
        loadFont("icons2", basePath + "icomoon_icon.ttf");
        loadFont("sf", basePath + "sf_symbols.ttf");
        // Chinese font fallback
        loadFont("chinese", basePath + "NotoSansSC-Regular.otf");
        loadFont("chinese-bold", basePath + "NotoSansSC-Bold.otf");
    }

    private void loadFont(String name, String resourcePath) {
        try {
            Path path = Paths.get("src/client/resources/" + resourcePath);
            if (!Files.exists(path)) {
                try (InputStream is = MinecraftClient.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) return;
                    byte[] bytes = is.readAllBytes();
                    ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
                    buffer.put(bytes);
                    buffer.flip();
                    int fontId = NanoVG.nvgCreateFontMem(ctx, name, buffer, false);
                    if (fontId != -1) {
                        fonts.put(name, fontId);
                    }
                }
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            int fontId = NanoVG.nvgCreateFontMem(ctx, name, buffer, false);
            if (fontId != -1) {
                fonts.put(name, fontId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getFont(String name) {
        return fonts.getOrDefault(name, -1);
    }

    public boolean hasFont(String name) {
        return fonts.containsKey(name);
    }

    public int loadImage(String name, String resourcePath) {
        if (!initialized || ctx == 0) {
            return -1;
        }
        if (images.containsKey(name)) {
            return images.get(name);
        }
        try {
            Path path = Paths.get("src/client/resources/" + resourcePath);
            byte[] bytes;
            if (!Files.exists(path)) {
                try (InputStream is = MinecraftClient.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) return -1;
                    bytes = is.readAllBytes();
                }
            } else {
                bytes = Files.readAllBytes(path);
            }
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            int img = NanoVG.nvgCreateImageMem(ctx, 0, buffer);
            MemoryUtil.memFree(buffer);
            if (img != 0) {
                images.put(name, img);
                return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getImage(String name) {
        return images.getOrDefault(name, -1);
    }

    public void getImageSize(String name, float[] size) {
        int img = getImage(name);
        if (img != -1 && size.length >= 2) {
            int[] w = new int[1], h = new int[1];
            NanoVG.nvgImageSize(ctx, img, w, h);
            size[0] = w[0];
            size[1] = h[0];
        }
    }

    public void beginFrame(int width, int height, float scale) {
        if (frameActive) return;
        NanoVG.nvgBeginFrame(ctx, width, height, scale);
        frameActive = true;
    }

    public void endFrame() {
        if (!frameActive) return;
        NanoVG.nvgEndFrame(ctx);
        frameActive = false;
    }

    public boolean isFrameActive() {
        return frameActive;
    }

    public static NVGColor color(float r, float g, float b, float a) {
        return NVGColor.calloc().r(r).g(g).b(b).a(a);
    }

    public static NVGColor color(int rgba) {
        float r = ((rgba >> 16) & 0xFF) / 255f;
        float g = ((rgba >> 8) & 0xFF) / 255f;
        float b = (rgba & 0xFF) / 255f;
        float a = ((rgba >> 24) & 0xFF) / 255f;
        return NVGColor.calloc().r(r).g(g).b(b).a(a);
    }
}
