#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler;

out vec4 OutColor;

#import <common.glsl>

void main() {
    float alpha = texture(Sampler, TexCoord).r;

    // alpha test
    if (alpha == 0.0) {
        discard;
    }

    OutColor = vec4(FragColor.rgb, FragColor.a * alpha);
}