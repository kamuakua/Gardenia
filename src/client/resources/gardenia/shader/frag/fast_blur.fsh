#version 150

in vec2 FragCoord;
in vec2 TexCoord;

uniform sampler2D iChannel0;

uniform float bluramount; // default 0.02
uniform float repeats;
uniform float factor;
uniform float repeatsInv;
uniform float Opacity;

uniform vec2 Texel;

uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Smoothness; // edge smoothness

out vec4 OutColor;

#import <common.glsl>

float rand(vec2 co) {
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

#define TO_DEGREES 57.29577951308232

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = fragCoord * Texel;

    vec3 blurred_image = vec3(0.);

    for (float i = 0.; i < repeats; i++) {
        float deg = (i * factor * 360.0) * TO_DEGREES;
        vec2 cs = vec2(cos(deg), sin(deg));

        vec2 q = cs * (rand(vec2(i,uv.x+uv.y))+bluramount);
        vec2 uv2 = uv+(q*bluramount);
        blurred_image += texture(iChannel0, uv2).rgb * 0.5;

        q = cs *  (rand(vec2(i+2.,uv.x+uv.y+24.))+bluramount);
        uv2 = uv+(q*bluramount);
        blurred_image += texture(iChannel0, uv2).rgb * 0.5;
    }
    blurred_image *= repeatsInv;

    fragColor = vec4(blurred_image, Opacity);
}

void main() {
    float dist = rsdf(Size, FragCoord, Radius);
    float smoothedAlpha = smoothen(dist, Smoothness);

    if (smoothedAlpha == 0.0) { // alpha test
        discard;
    }

    mainImage(OutColor, gl_FragCoord.xy);
    OutColor.a *= smoothedAlpha;
}