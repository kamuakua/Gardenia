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

vec4 tex(vec2 uv) {
    vec2 res = textureSize(Sampler, 0);
    uv = uv*res + 0.5;

    // tweak fractionnal value of the texture coordinate
    vec2 fl = floor(uv);
    vec2 fr = fract(uv);
    vec2 aa = fwidth(uv)*0.75;
    fr = smoothstep( vec2(0.5)-aa, vec2(0.5)+aa, fr);

    uv = (fl+fr-0.5) / res;
    return texture(Sampler, uv);
}

void main() {
    float dist = rsdf(Size, FragCoord, Radius);
    float smoothedAlpha = smoothen(dist, Smoothness);

    if (smoothedAlpha == 0.0) { // alpha test
        discard;
    }

    vec4 fragColor = tex(TexCoord);
    if (fragColor.a == 0.0) { // alpha test
        discard;
    }

    vec4 color = vec4(fragColor.rgb, fragColor.a * smoothedAlpha * Opacity);

    OutColor = color;
}