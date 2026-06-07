package cn.gardenia.client.gui.animation;

import cn.gardenia.client.gui.nanovg.NanoVGManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HelloAnimation {
    private static final float DRAW_DURATION = 1.8f;
    private static final float HOLD_DURATION = 0.4f;
    private static final float FADE_DURATION = 0.5f;
    private static final int BEZIER_SAMPLES = 50;

    private long startTime = -1;
    private boolean finished = false;
    private boolean loaded = false;

    private float viewBoxW, viewBoxH;
    private float transformTx, transformTy;
    private final List<float[]> pathPoints = new ArrayList<>();
    private final List<Float> pathLengths = new ArrayList<>();
    private float totalLength = 0f;

    public HelloAnimation() {
        load();
    }

    private void load() {
        try {
            String content = null;
            Path path = Paths.get("src/client/resources/gardenia/animation/hello/hello.json");
            if (Files.exists(path)) {
                content = Files.readString(path);
            } else {
                InputStream is = MinecraftClient.class.getClassLoader()
                        .getResourceAsStream("gardenia/animation/hello/hello.json");
                if (is != null) {
                    content = new String(is.readAllBytes());
                    is.close();
                }
            }
            if (content == null) {
                finished = true;
                return;
            }

            JsonObject json = new Gson().fromJson(content, JsonObject.class);
            viewBoxW = json.getAsJsonObject("viewBox").get("w").getAsFloat();
            viewBoxH = json.getAsJsonObject("viewBox").get("h").getAsFloat();
            transformTx = json.getAsJsonObject("transform").get("tx").getAsFloat();
            transformTy = json.getAsJsonObject("transform").get("ty").getAsFloat();

            for (var element : json.getAsJsonArray("paths")) {
                String pathStr = element.getAsString();
                List<PathSegment> segments = parseSvgPath(pathStr);
                float[] sampled = samplePath(segments);
                if (sampled.length >= 4) {
                    pathPoints.add(sampled);
                    float len = calculatePathLength(sampled);
                    pathLengths.add(len);
                    totalLength += len;
                }
            }

            loaded = !pathPoints.isEmpty();
            if (!loaded) finished = true;
        } catch (Exception e) {
            finished = true;
        }
    }

    public boolean update() {
        if (finished) return true;
        if (startTime < 0) startTime = System.nanoTime();
        float elapsed = getElapsedTime();
        if (elapsed >= DRAW_DURATION + HOLD_DURATION + FADE_DURATION) {
            finished = true;
        }
        return finished;
    }

    public void render(float screenW, float screenH, boolean isDarkTheme) {
        if (!loaded || finished) return;

        float elapsed = getElapsedTime();
        float drawProgress = Math.min(1f, elapsed / DRAW_DURATION);
        float fadeAlpha = 1f;
        if (elapsed > DRAW_DURATION + HOLD_DURATION) {
            fadeAlpha = 1f - (elapsed - DRAW_DURATION - HOLD_DURATION) / FADE_DURATION;
            fadeAlpha = Math.max(0f, fadeAlpha);
        }

        drawProgress = easeOutCubic(drawProgress);

        float scale = Math.min(screenW * 0.55f / viewBoxW, screenH * 0.25f / viewBoxH);
        float offsetX = (screenW - viewBoxW * scale) / 2f;
        float offsetY = screenH * 0.4f - viewBoxH * scale / 2f;

        long ctx = NanoVGManager.INSTANCE.getContext();
        NanoVG.nvgSave(ctx);
        NanoVG.nvgGlobalAlpha(ctx, fadeAlpha);

        float drawLength = drawProgress * totalLength;
        float accumulated = 0f;

        for (int i = 0; i < pathPoints.size(); i++) {
            if (accumulated >= drawLength) break;
            float[] points = pathPoints.get(i);
            float pathLen = pathLengths.get(i);
            float remaining = drawLength - accumulated;
            float drawLen = Math.min(pathLen, remaining);
            float progress = pathLen > 0 ? drawLen / pathLen : 1f;
            renderPath(ctx, points, progress, scale, offsetX, offsetY, isDarkTheme);
            accumulated += pathLen;
        }

        NanoVG.nvgRestore(ctx);
    }

    private void renderPath(long ctx, float[] points, float progress, float scale, float offsetX, float offsetY, boolean isDarkTheme) {
        int pointCount = points.length / 2;
        if (pointCount < 2) return;

        int drawCount = Math.max(2, (int) (pointCount * progress));

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgMoveTo(ctx, points[0] * scale + offsetX, points[1] * scale + offsetY);
        for (int i = 1; i < drawCount; i++) {
            NanoVG.nvgLineTo(ctx, points[i * 2] * scale + offsetX, points[i * 2 + 1] * scale + offsetY);
        }

        int glowColor = isDarkTheme ? 0x307BA8FF : 0x206496FF;
        NVGColor glow = NanoVGManager.color(glowColor);
        NanoVG.nvgStrokeColor(ctx, glow);
        NanoVG.nvgStrokeWidth(ctx, 8f);
        NanoVG.nvgLineCap(ctx, NanoVG.NVG_ROUND);
        NanoVG.nvgLineJoin(ctx, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(ctx);
        glow.free();

        NanoVG.nvgBeginPath(ctx);
        NanoVG.nvgMoveTo(ctx, points[0] * scale + offsetX, points[1] * scale + offsetY);
        for (int i = 1; i < drawCount; i++) {
            NanoVG.nvgLineTo(ctx, points[i * 2] * scale + offsetX, points[i * 2 + 1] * scale + offsetY);
        }

        int strokeColor = isDarkTheme ? 0xF0FFFFFF : 0xF0333333;
        NVGColor stroke = NanoVGManager.color(strokeColor);
        NanoVG.nvgStrokeColor(ctx, stroke);
        NanoVG.nvgStrokeWidth(ctx, 3f);
        NanoVG.nvgLineCap(ctx, NanoVG.NVG_ROUND);
        NanoVG.nvgLineJoin(ctx, NanoVG.NVG_ROUND);
        NanoVG.nvgStroke(ctx);
        stroke.free();

        if (progress < 0.99f && drawCount > 0) {
            int lastIdx = (drawCount - 1) * 2;
            float tipX = points[lastIdx] * scale + offsetX;
            float tipY = points[lastIdx + 1] * scale + offsetY;

            NVGColor tipGlow = NanoVGManager.color(0.5f, 0.7f, 1f, 0.5f);
            NanoVG.nvgBeginPath(ctx);
            NanoVG.nvgCircle(ctx, tipX, tipY, 6);
            NanoVG.nvgFillColor(ctx, tipGlow);
            NanoVG.nvgFill(ctx);
            tipGlow.free();

            NVGColor tipCore = NanoVGManager.color(1f, 1f, 1f, 0.85f);
            NanoVG.nvgBeginPath(ctx);
            NanoVG.nvgCircle(ctx, tipX, tipY, 2);
            NanoVG.nvgFillColor(ctx, tipCore);
            NanoVG.nvgFill(ctx);
            tipCore.free();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isDrawComplete() {
        if (startTime < 0) return false;
        return getElapsedTime() >= DRAW_DURATION;
    }

    private float getElapsedTime() {
        if (startTime < 0) return 0f;
        return (System.nanoTime() - startTime) / 1_000_000_000f;
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private float calculatePathLength(float[] points) {
        float len = 0;
        for (int i = 2; i < points.length; i += 2) {
            float dx = points[i] - points[i - 2];
            float dy = points[i + 1] - points[i - 1];
            len += (float) Math.sqrt(dx * dx + dy * dy);
        }
        return len;
    }

    private float[] samplePath(List<PathSegment> segments) {
        List<Float> points = new ArrayList<>();
        float curX = 0, curY = 0;

        for (PathSegment seg : segments) {
            if (seg instanceof MoveToSeg) {
                MoveToSeg m = (MoveToSeg) seg;
                curX = m.x;
                curY = m.y;
                points.add(curX + transformTx);
                points.add(curY + transformTy);
            } else if (seg instanceof LineToSeg) {
                LineToSeg l = (LineToSeg) seg;
                int samples = Math.max(2, BEZIER_SAMPLES / 5);
                for (int i = 1; i <= samples; i++) {
                    float t = (float) i / samples;
                    float x = curX + (l.x - curX) * t + transformTx;
                    float y = curY + (l.y - curY) * t + transformTy;
                    points.add(x);
                    points.add(y);
                }
                curX = l.x;
                curY = l.y;
            } else if (seg instanceof CubicBezierSeg) {
                CubicBezierSeg cb = (CubicBezierSeg) seg;
                for (int i = 1; i <= BEZIER_SAMPLES; i++) {
                    float t = (float) i / BEZIER_SAMPLES;
                    float u = 1 - t;
                    float x = u * u * u * curX + 3 * u * u * t * cb.cp1x + 3 * u * t * t * cb.cp2x + t * t * t * cb.ex + transformTx;
                    float y = u * u * u * curY + 3 * u * u * t * cb.cp1y + 3 * u * t * t * cb.cp2y + t * t * t * cb.ey + transformTy;
                    points.add(x);
                    points.add(y);
                }
                curX = cb.ex;
                curY = cb.ey;
            }
        }

        float[] result = new float[points.size()];
        for (int i = 0; i < points.size(); i++) result[i] = points.get(i);
        return result;
    }

    // ==================== SVG Path Parsing ====================

    private List<PathSegment> parseSvgPath(String d) {
        List<Object> tokens = new ArrayList<>();
        int i = 0;
        while (i < d.length()) {
            char c = d.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }
            if (Character.isLetter(c)) {
                tokens.add(c);
                i++;
                continue;
            }
            int start = i;
            if (c == '-' || c == '+') i++;
            boolean hasDot = false;
            while (i < d.length()) {
                char ch = d.charAt(i);
                if (Character.isDigit(ch)) {
                    i++;
                } else if (ch == '.' && !hasDot) {
                    hasDot = true;
                    i++;
                } else {
                    break;
                }
            }
            if (i > start) {
                tokens.add(Float.parseFloat(d.substring(start, i)));
            } else {
                i++;
            }
        }

        List<PathSegment> segments = new ArrayList<>();
        int pos = 0;
        float curX = 0, curY = 0;
        float lastCpX = 0, lastCpY = 0;
        boolean lastWasCubic = false;
        char impliedCmd = 'M';

        while (pos < tokens.size()) {
            Object t = tokens.get(pos);
            char cmd;
            if (t instanceof Character) {
                cmd = (Character) t;
                pos++;
                impliedCmd = cmd;
                if (cmd == 'M') impliedCmd = 'L';
                if (cmd == 'm') impliedCmd = 'l';
            } else {
                cmd = impliedCmd;
            }

            switch (cmd) {
                case 'M': {
                    float x = nf(tokens, pos++);
                    float y = nf(tokens, pos++);
                    segments.add(new MoveToSeg(x, y));
                    curX = x;
                    curY = y;
                    impliedCmd = 'L';
                    lastWasCubic = false;
                    break;
                }
                case 'm': {
                    float x = nf(tokens, pos++) + curX;
                    float y = nf(tokens, pos++) + curY;
                    segments.add(new MoveToSeg(x, y));
                    curX = x;
                    curY = y;
                    impliedCmd = 'l';
                    lastWasCubic = false;
                    break;
                }
                case 'L': {
                    float x = nf(tokens, pos++);
                    float y = nf(tokens, pos++);
                    segments.add(new LineToSeg(x, y));
                    curX = x;
                    curY = y;
                    lastWasCubic = false;
                    break;
                }
                case 'l': {
                    float x = nf(tokens, pos++) + curX;
                    float y = nf(tokens, pos++) + curY;
                    segments.add(new LineToSeg(x, y));
                    curX = x;
                    curY = y;
                    lastWasCubic = false;
                    break;
                }
                case 'C': {
                    float cp1x = nf(tokens, pos++);
                    float cp1y = nf(tokens, pos++);
                    float cp2x = nf(tokens, pos++);
                    float cp2y = nf(tokens, pos++);
                    float ex = nf(tokens, pos++);
                    float ey = nf(tokens, pos++);
                    segments.add(new CubicBezierSeg(cp1x, cp1y, cp2x, cp2y, ex, ey));
                    lastCpX = cp2x;
                    lastCpY = cp2y;
                    curX = ex;
                    curY = ey;
                    lastWasCubic = true;
                    break;
                }
                case 'c': {
                    float cp1x = nf(tokens, pos++) + curX;
                    float cp1y = nf(tokens, pos++) + curY;
                    float cp2x = nf(tokens, pos++) + curX;
                    float cp2y = nf(tokens, pos++) + curY;
                    float ex = nf(tokens, pos++) + curX;
                    float ey = nf(tokens, pos++) + curY;
                    segments.add(new CubicBezierSeg(cp1x, cp1y, cp2x, cp2y, ex, ey));
                    lastCpX = cp2x;
                    lastCpY = cp2y;
                    curX = ex;
                    curY = ey;
                    lastWasCubic = true;
                    break;
                }
                case 'S': {
                    float cp1x = lastWasCubic ? 2 * curX - lastCpX : curX;
                    float cp1y = lastWasCubic ? 2 * curY - lastCpY : curY;
                    float cp2x = nf(tokens, pos++);
                    float cp2y = nf(tokens, pos++);
                    float ex = nf(tokens, pos++);
                    float ey = nf(tokens, pos++);
                    segments.add(new CubicBezierSeg(cp1x, cp1y, cp2x, cp2y, ex, ey));
                    lastCpX = cp2x;
                    lastCpY = cp2y;
                    curX = ex;
                    curY = ey;
                    lastWasCubic = true;
                    break;
                }
                case 's': {
                    float cp1x = lastWasCubic ? 2 * curX - lastCpX : curX;
                    float cp1y = lastWasCubic ? 2 * curY - lastCpY : curY;
                    float cp2x = nf(tokens, pos++) + curX;
                    float cp2y = nf(tokens, pos++) + curY;
                    float ex = nf(tokens, pos++) + curX;
                    float ey = nf(tokens, pos++) + curY;
                    segments.add(new CubicBezierSeg(cp1x, cp1y, cp2x, cp2y, ex, ey));
                    lastCpX = cp2x;
                    lastCpY = cp2y;
                    curX = ex;
                    curY = ey;
                    lastWasCubic = true;
                    break;
                }
                case 'Z':
                case 'z': {
                    lastWasCubic = false;
                    break;
                }
                default:
                    break;
            }
        }

        return segments;
    }

    private float nf(List<Object> tokens, int pos) {
        if (pos >= tokens.size()) return 0f;
        Object t = tokens.get(pos);
        return t instanceof Float ? (Float) t : 0f;
    }

    // ==================== Segment Types ====================

    private static class PathSegment {
    }

    private static class MoveToSeg extends PathSegment {
        float x, y;

        MoveToSeg(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class LineToSeg extends PathSegment {
        float x, y;

        LineToSeg(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class CubicBezierSeg extends PathSegment {
        float cp1x, cp1y, cp2x, cp2y, ex, ey;

        CubicBezierSeg(float cp1x, float cp1y, float cp2x, float cp2y, float ex, float ey) {
            this.cp1x = cp1x;
            this.cp1y = cp1y;
            this.cp2x = cp2x;
            this.cp2y = cp2y;
            this.ex = ex;
            this.ey = ey;
        }
    }
}
