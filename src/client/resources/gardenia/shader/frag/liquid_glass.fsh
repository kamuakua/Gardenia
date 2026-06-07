#version 150

in vec2 FragCoord;
in vec2 TexCoord;

uniform sampler2D iChannel0;
uniform vec2 Resolution;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float BlurRadius;

out vec4 OutColor;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;
    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float rsdf(vec2 size, vec2 coord, vec4 radius) {
    vec2 center = size * 0.5;
    return rdist(center - (coord * size), center - 1.0, radius);
}

float smoothen(float dist, float smoothness) {
    return 1.0 - smoothstep(1.0 - smoothness * 2.0, 1.0, dist);
}

vec2 surfaceNormal(vec2 uv) {
    vec2 texel = 1.0 / Resolution;
    float h = sin(uv.x * 12.0 + uv.y * 8.0) * 0.002;
    float v = cos(uv.x * 8.0 - uv.y * 12.0) * 0.002;
    return normalize(vec2(h, v));
}

void main() {
    float dist = rsdf(Size, FragCoord, Radius);
    float smoothedAlpha = smoothen(dist, Smoothness);

    if (smoothedAlpha == 0.0) {
        discard;
    }

    vec2 texel = 1.0 / Resolution;
    vec2 normal = surfaceNormal(TexCoord);
    vec2 refractionOffset = normal * 0.008;

    vec2 uv = TexCoord + refractionOffset;

    float totalWeight = 0.0;
    vec3 blurred = vec3(0.0);

    float aspect = Resolution.x / Resolution.y;
    float radius = BlurRadius * texel.x;

    const float PI = 3.14159265;
    const int DIRECTIONS = 16;
    const int SAMPLES = 6;

    for (int d = 0; d < DIRECTIONS; d++) {
        float angle = float(d) * (2.0 * PI / float(DIRECTIONS));
        vec2 dir = vec2(cos(angle), sin(angle));
        dir.y /= aspect;

        for (int s = 1; s <= SAMPLES; s++) {
            float fi = float(s) / float(SAMPLES);
            float weight = 1.0 - fi * 0.5;
            vec2 offset = dir * radius * fi;
            blurred += texture(iChannel0, clamp(uv + offset, 0.0, 1.0)).rgb * weight;
            totalWeight += weight;
        }
    }

    vec3 center = texture(iChannel0, clamp(uv, 0.0, 1.0)).rgb;
    blurred += center * 2.0;
    totalWeight += 2.0;
    blurred /= totalWeight;

    float caStrength = 0.004;
    float rSample = texture(iChannel0, clamp(uv + refractionOffset * 0.5 + vec2(caStrength, 0.0), 0.0, 1.0)).r;
    float gSample = blurred.g;
    float bSample = texture(iChannel0, clamp(uv + refractionOffset * 0.5 - vec2(0.0, caStrength), 0.0, 1.0)).b;

    vec3 chromatic = vec3(rSample, gSample, bSample);

    float blendFactor = 0.6;
    vec3 result = mix(blurred, chromatic, blendFactor);

    vec2 edgeDist = abs(FragCoord - 0.5) * 2.0;
    float edgeFade = smoothstep(0.7, 1.0, max(edgeDist.x, edgeDist.y));
    result += vec3(0.03) * edgeFade;

    float centerDist = length(FragCoord - vec2(0.3, 0.25));
    float specular = exp(-centerDist * centerDist * 15.0) * 0.08;
    result += vec3(specular);

    result *= vec3(0.95, 0.97, 1.0);

    float fresnel = 1.0 - abs(dot(normalize(vec3(normal, 1.0)), vec3(0.0, 0.0, 1.0)));
    fresnel = pow(fresnel, 3.0) * 0.15;
    result += vec3(fresnel);

    float edgeHighlight = exp(-dist * 0.15) * 0.03;
    result += vec3(edgeHighlight);

    result = clamp(result, 0.0, 1.0);

    OutColor = vec4(result, smoothedAlpha);
}
