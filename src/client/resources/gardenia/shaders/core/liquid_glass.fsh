#version 150

#moj_import <gardenia:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float BlurRadius;

out vec4 OutColor;

const float DPI = 6.28318530718;
const float STEP = DPI / 16.0;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    vec2 texSize = textureSize(Sampler0, 0);

    // ============ SIMPLE BLUR (full area) ============
    vec3 blurred = vec3(0.0);
    float totalWeight = 0.0;
    float stepSize = BlurRadius / texSize.x;

    blurred += texture(Sampler0, TexCoord).rgb * 2.0;
    totalWeight += 2.0;

    for (float d = 0.0; d < DPI; d += STEP) {
        vec2 dir = vec2(cos(d), sin(d));
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            float weight = 1.0 - i * 0.5;
            blurred += texture(Sampler0, TexCoord + dir * stepSize * i).rgb * weight;
            totalWeight += weight;
        }
    }
    blurred /= totalWeight;

    // ============ NOISE (interior) ============
    vec2 noiseCoord = TexCoord * texSize / 48.0;
    float grain = (noise(noiseCoord) - 0.5) * 0.04;
    vec3 interiorColor = blurred + grain;

    // Edge effects with chromatic dispersion only
    vec2 center = Size * 0.5;
    float sdfDist = rdist(center - FragCoord * Size, center - 1.0, Radius);
    float edgeDist = -sdfDist;
    float edgeWidth = 100.0;
    float edgeFactor = 1.0 - smoothstep(0.0, edgeWidth, edgeDist);

    vec2 gradient = vec2(dFdx(edgeDist), dFdy(edgeDist));
    float gradLen = length(gradient);
    vec2 refractionDir = gradLen > 0.001 ? normalize(-gradient) : vec2(0.0);

    float refractionStrength = smoothstep(0.0, 15.0, edgeDist) * (1.0 - smoothstep(15.0, edgeWidth * 0.5, edgeDist));
    vec2 refractionOffset = refractionDir * refractionStrength * 24.0 / texSize;

    float caStrength = 8.0 / texSize.x;
    vec2 tcR = clamp(TexCoord + refractionOffset + vec2(caStrength, 0.0), vec2(0.0), vec2(1.0));
    vec2 tcG = clamp(TexCoord + refractionOffset, vec2(0.0), vec2(1.0));
    vec2 tcB = clamp(TexCoord + refractionOffset - vec2(caStrength, 0.0), vec2(0.0), vec2(1.0));

    vec3 edgeColor;
    edgeColor.r = texture(Sampler0, tcR).r;
    edgeColor.g = texture(Sampler0, tcG).g;
    edgeColor.b = texture(Sampler0, tcB).b;

    edgeColor = mix(edgeColor, blurred, 0.3);

    // ============ BLEND ============
    vec3 finalColor = mix(interiorColor, edgeColor, edgeFactor);
    finalColor = clamp(finalColor, 0.0, 1.0);

    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    if (alpha == 0.0) discard;

    OutColor = vec4(finalColor, 1.0) * FragColor;
}