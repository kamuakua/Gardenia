#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;

uniform sampler2D Sampler;
uniform vec2 Resolution;
uniform vec2 Size;
uniform vec2 TexelSize;
uniform vec2 Direction;

uniform float Opacity;
uniform float Strength;
uniform float Smoothness;
uniform float Brightness;
uniform vec4 Radius;

uniform float Kernel[256];

out vec4 OutColor;

#define Offset TexelSize * Direction

#import <common.glsl>

void main() {
    float smoothedAlpha = smoothen(rsdf(Size, FragCoord, Radius), Smoothness);
    if (smoothedAlpha == 0.0) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / Resolution.xy;

    vec4 color = texture2D(Sampler, uv) * Kernel[0];

    for (float f = 1.0; f <= Strength; f++) {
        color += texture(Sampler, uv + f * Offset) * (Kernel[int(abs(f))]);
        color += texture(Sampler, uv - f * Offset) * (Kernel[int(abs(f))]);
    }

    OutColor = vec4(color.rgb * Brightness, color.a * Opacity * smoothedAlpha);
}