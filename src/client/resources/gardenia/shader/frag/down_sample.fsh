#version 150

in vec2 FragCoord;
in vec2 TexCoord;

uniform sampler2D iChannel0;
uniform vec2 halfpixel;
uniform float offset;

out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    vec4 sum = texture(iChannel0, uv) * 4.0;

    sum += texture(iChannel0, uv - halfpixel.xy * offset);
    sum += texture(iChannel0, uv + halfpixel.xy * offset);
    sum += texture(iChannel0, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
    sum += texture(iChannel0, uv - vec2(halfpixel.x, -halfpixel.y) * offset);

    OutColor = vec4(sum.rgb / 8.0, 1.0);
}