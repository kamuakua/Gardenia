#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive

uniform vec2 Size; // rectangle size
uniform vec4 Color1, Color2, Color3, Color4; // bottomLeft, topLeft, bottomRight, topRight
uniform vec4 Radius; // radius for each vertex
uniform float Smoothness; // edge smoothness
uniform bool Unfilled;

out vec4 OutColor;

#import <common.glsl>

void main() {
    float dist = rsdf(Size, FragCoord, Radius);
    float smoothedAlpha = smoothen(dist, Smoothness);

    if (Unfilled) {
        float innerAlpha = 1.0 - ralpha(Size - Smoothness * 2, FragCoord, Radius, 1.0);
        smoothedAlpha *= innerAlpha;
    }

    vec4 fragColor = createGradient(FragCoord, Color1, Color2, Color3, Color4);
    vec4 color = vec4(fragColor.rgb, fragColor.a * smoothedAlpha);

    if (smoothedAlpha == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}