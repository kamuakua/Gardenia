#version 150

in vec2 FragCoord;

uniform float iTime;
uniform float Scale; // Default value: 4.0
uniform vec2 Size;
uniform vec4 Color;
uniform float Gap; // Default value: 0.02

out vec4 OutColor;

void main() {
    //vec2 px = Scale * (-Size.xy + 2.0 * gl_FragCoord.xy) / Size.y;
    vec2 px = Scale * FragCoord * Size.xy / Size.y;
    float id = 0.5 + 0.5 * cos(iTime + sin(dot(floor(px + 0.5), vec2(113.1, 17.81))) * 43758.545);
    vec2 pa = smoothstep(0.0, Gap, (0.5 + 0.5*cos(6.2831*px)));

    OutColor = vec4( Color.rgb, Color.a * id * pa.x * pa.y);
}