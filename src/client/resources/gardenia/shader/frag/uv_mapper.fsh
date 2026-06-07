#version 150

in vec2 FragCoord;

uniform vec2 Resolution;
uniform sampler2D iChannel0;
uniform float Opacity;

out vec4 OutColor;

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec4 color = texture(iChannel0, uv);
    OutColor = vec4(color.rgb, color.a * Opacity);
}