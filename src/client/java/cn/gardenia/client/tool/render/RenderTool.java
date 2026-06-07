package cn.gardenia.client.tool.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import java.awt.Color;

public class RenderTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    private void drawBuffer(BufferBuilder buffer) {
        BuiltBuffer built = buffer.end();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    public void drawLine(MatrixStack matrices, Vec3d start, Vec3d end, Color color, float width) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
        buffer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)end.x, (float)end.y, (float)end.z).color(r, g, b, a);
        drawBuffer(buffer);
    }

    public void drawBox(MatrixStack matrices, Box box, Color color, boolean fill) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexFormat.DrawMode mode = fill ? VertexFormat.DrawMode.TRIANGLE_STRIP : VertexFormat.DrawMode.DEBUG_LINES;
        BufferBuilder buffer = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        float minX = (float)box.minX, minY = (float)box.minY, minZ = (float)box.minZ;
        float maxX = (float)box.maxX, maxY = (float)box.maxY, maxZ = (float)box.maxZ;

        if (fill) {
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        } else {
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        }

        drawBuffer(buffer);
    }

    public void drawCircle(MatrixStack matrices, Vec3d center, double radius, Color color, int segments, boolean fill) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexFormat.DrawMode mode = fill ? VertexFormat.DrawMode.TRIANGLE_FAN : VertexFormat.DrawMode.DEBUG_LINE_STRIP;
        BufferBuilder buffer = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        if (fill) {
            buffer.vertex(matrix, (float)center.x, (float)center.y, (float)center.z).color(r, g, b, a);
        }
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            buffer.vertex(matrix, (float)(center.x + radius * Math.cos(angle)), (float)center.y, (float)(center.z + radius * Math.sin(angle))).color(r, g, b, a);
        }
        drawBuffer(buffer);
    }

    public void drawSphere(MatrixStack matrices, Vec3d center, double radius, Color color, int stacks, int slices) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        for (int i = 0; i < stacks; i++) {
            double phi1 = Math.PI * i / stacks, phi2 = Math.PI * (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                double theta1 = 2 * Math.PI * j / slices, theta2 = 2 * Math.PI * (j + 1) / slices;
                buffer.vertex(matrix, (float)(center.x + radius * Math.sin(phi1) * Math.cos(theta1)), (float)(center.y + radius * Math.cos(phi1)), (float)(center.z + radius * Math.sin(phi1) * Math.sin(theta1))).color(r, g, b, a);
                buffer.vertex(matrix, (float)(center.x + radius * Math.sin(phi1) * Math.cos(theta2)), (float)(center.y + radius * Math.cos(phi1)), (float)(center.z + radius * Math.sin(phi1) * Math.sin(theta2))).color(r, g, b, a);
                buffer.vertex(matrix, (float)(center.x + radius * Math.sin(phi1) * Math.cos(theta1)), (float)(center.y + radius * Math.cos(phi1)), (float)(center.z + radius * Math.sin(phi1) * Math.sin(theta1))).color(r, g, b, a);
                buffer.vertex(matrix, (float)(center.x + radius * Math.sin(phi2) * Math.cos(theta1)), (float)(center.y + radius * Math.cos(phi2)), (float)(center.z + radius * Math.sin(phi2) * Math.sin(theta1))).color(r, g, b, a);
            }
        }
        drawBuffer(buffer);
    }

    public void drawCrossHair(MatrixStack matrices, Vec3d center, float size, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        buffer.vertex(matrix, (float)center.x - size, (float)center.y, (float)center.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)center.x + size, (float)center.y, (float)center.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)center.x, (float)center.y - size, (float)center.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)center.x, (float)center.y + size, (float)center.z).color(r, g, b, a);

        drawBuffer(buffer);
    }

    public void drawBezierCurve(MatrixStack matrices, Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, Color color, int segments) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments, mt = 1.0 - t;
            buffer.vertex(matrix,
                    (float)(mt * mt * mt * p0.x + 3 * mt * mt * t * p1.x + 3 * mt * t * t * p2.x + t * t * t * p3.x),
                    (float)(mt * mt * mt * p0.y + 3 * mt * mt * t * p1.y + 3 * mt * t * t * p2.y + t * t * t * p3.y),
                    (float)(mt * mt * mt * p0.z + 3 * mt * mt * t * p1.z + 3 * mt * t * t * p2.z + t * t * t * p3.z)
            ).color(r, g, b, a);
        }
        drawBuffer(buffer);
    }

    public Box entityToBox(Vec3d pos, double width, double height) {
        double hw = width / 2.0;
        return new Box(pos.x - hw, pos.y, pos.z - hw, pos.x + hw, pos.y + height, pos.z + hw);
    }

    public Vec3d cameraToWorldPos(double screenX, double screenY, double depth) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec2f rotation = new Vec2f(camera.getYaw(), camera.getPitch());
        double radPitch = Math.toRadians(rotation.y);
        double radYaw = Math.toRadians(rotation.x);
        double hFov = Math.toRadians(mc.options.getFov().getValue()) / 2.0;
        double vFov = hFov * mc.getWindow().getHeight() / mc.getWindow().getWidth();

        double pitchOffset = Math.tan(vFov * (1.0 - screenY * 2.0));
        double yawOffset = Math.tan(hFov * (screenX * 2.0 - 1.0));

        Vec3d forward = new Vec3d(-Math.sin(radYaw) * Math.cos(radPitch), -Math.sin(radPitch), Math.cos(radYaw) * Math.cos(radPitch));
        Vec3d right = new Vec3d(Math.cos(radYaw), 0, Math.sin(radYaw));
        Vec3d up = forward.crossProduct(right).normalize();

        Vec3d dir = forward.add(right.multiply(yawOffset)).add(up.multiply(pitchOffset)).normalize();
        return camPos.add(dir.multiply(depth));
    }
}
