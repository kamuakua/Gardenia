package cn.gardenia.client.tool.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShaderTool {
    public void init() {}

    public void setShaderColor(float r, float g, float b, float a) {
        RenderSystem.setShaderColor(r, g, b, a);
    }

    public void resetShaderColor() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void setShaderTexture(int unit, Identifier id) {
        RenderSystem.setShaderTexture(unit, id);
    }

    public ShaderProgram getActiveProgram() {
        return RenderSystem.getShader();
    }

    public void setUniformMat4(String name, Matrix4f matrix) {
        ShaderProgram program = RenderSystem.getShader();
        if (program != null) {
            Uniform uniform = program.getUniform(name);
            if (uniform != null) uniform.set(matrix);
        }
    }

    public void setUniformVec3(String name, Vector3f vec) {
        ShaderProgram program = RenderSystem.getShader();
        if (program != null) {
            Uniform uniform = program.getUniform(name);
            if (uniform != null) uniform.set(vec);
        }
    }

    public void setUniformFloat(String name, float value) {
        ShaderProgram program = RenderSystem.getShader();
        if (program != null) {
            Uniform uniform = program.getUniform(name);
            if (uniform != null) uniform.set(value);
        }
    }

    public void setUniformInt(String name, int value) {
        ShaderProgram program = RenderSystem.getShader();
        if (program != null) {
            Uniform uniform = program.getUniform(name);
            if (uniform != null) uniform.set(value);
        }
    }
}
