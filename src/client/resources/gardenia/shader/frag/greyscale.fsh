#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler;

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
    float alpha = tex(TexCoord).r;

    // alpha test
    if (alpha == 0.0) {
        discard;
    }

    OutColor = vec4(FragColor.rgb, FragColor.a * alpha);
}