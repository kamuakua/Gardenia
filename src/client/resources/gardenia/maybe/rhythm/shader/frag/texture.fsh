#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;

uniform sampler2D Sampler;
uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Smoothness; // edge smoothness
uniform float Opacity; // edge smoothness

out vec4 OutColor;

#import <common.glsl>

void main() {
    float dist = rsdf(Size, FragCoord, Radius);
    float smoothedAlpha = smoothen(dist, Smoothness);

    if (smoothedAlpha == 0.0) { // alpha test
        discard;
    }

    vec4 fragColor = texture(Sampler, TexCoord);
    vec4 color = vec4(fragColor.rgb, fragColor.a * smoothedAlpha * Opacity);

    OutColor = color;
}