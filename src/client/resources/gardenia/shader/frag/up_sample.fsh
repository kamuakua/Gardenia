#version 150

in vec2 FragCoord;
in vec2 TexCoord;

uniform sampler2D iChannel0;
uniform vec2 halfpixel;
uniform float offset;

out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    vec4 sum = texture(iChannel0, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);

    sum += texture(iChannel0, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(iChannel0, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
    sum += texture(iChannel0, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(iChannel0, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
    sum += texture(iChannel0, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
    sum += texture(iChannel0, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
    sum += texture(iChannel0, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;

    OutColor = vec4(sum.rgb / 12.0, 1.0);
}