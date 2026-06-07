#version 150

in vec2 FragCoord;
in vec2 TexCoord;

uniform sampler2D DiffuseSampler;

uniform vec2 Texel;

uniform vec2 Direction;
uniform float Opacity;
uniform int NumPairs;
uniform float Kernel[256];
uniform float KernelNorm;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float Brightness;

out vec4 FragColor;

#import <common.glsl>

void blurMain(out vec4 color, in vec2 uv) {
    vec3 blurred = texture(DiffuseSampler, uv).rgb * Kernel[0];
    for (int i = 0; i < NumPairs; i++) {
        float offset = Kernel[1 + i * 2];
        float w = Kernel[2 + i * 2];
        vec2 d = Texel * offset * Direction;
        blurred += (texture(DiffuseSampler, clamp(uv + d, vec2(0.0), vec2(1.0))).rgb +
                    texture(DiffuseSampler, clamp(uv - d, vec2(0.0), vec2(1.0))).rgb) * w;
    }
    color = vec4(blurred * KernelNorm, 1.0);
}

void main() {
    blurMain(FragColor, TexCoord);

    if (Direction.y > 0.0) {
        float dist = rsdf(Size, FragCoord, Radius);
        float smoothedAlpha = smoothen(dist, Smoothness);

        if (smoothedAlpha == 0.0) {
            discard;
        }

        FragColor.rgb *= Brightness;
        FragColor.a *= smoothedAlpha * Opacity;
    }
}