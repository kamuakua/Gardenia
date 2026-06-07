#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform float Size;
uniform float Radius;
uniform float Thickness;
uniform float Percent; // 0 to 1
uniform float Smoothness;
uniform vec4 LeftColor, RightColor;
uniform float Offset;

out vec4 OutColor;

#define PI 3.14159265359
#define NOISE 0.5 / 255.0

// ra: radius
// rb: thickness
float sdArc( in vec2 p, in vec2 sc, in float ra, float rb ){
    // sc is the sin/cos of the arc's aperture
    p.x = abs(p.x);
    return ((sc.y*p.x>sc.x*p.y) ? length(p-sc*ra) : abs(length(p)-ra)) - rb;
}

void main() {
	vec2 halfSize = vec2(Size * .5);
	vec2 center = halfSize - (FragCoord * Size);

	float rad = PI * Percent;
	float smoothedAlpha = (1.0 - smoothstep(1.0 - Smoothness, 1.0, sdArc(center, vec2(sin(rad),cos(rad)), Radius - 1.0, Thickness))) * FragColor.a;

    if (smoothedAlpha == 0.0) {
        discard;
    }

    float angle = atan(center.x, center.y);
    float factor = mod((angle + PI) + Offset, 2.0 * PI) / (2.0 * PI);
    vec3 color = mix(LeftColor.rgb, RightColor.rgb, factor);
    color += mix(NOISE, -NOISE, fract(sin(dot(vec2(factor), vec2(12.9898, 78.233))) * 43758.5453));

	OutColor = vec4(color, smoothedAlpha);
}