#version 150

in vec2 TexCoord;
in vec2 FragCoord;

uniform sampler2D Source;
uniform vec2 Resolution;
uniform float InvResolution;
uniform float AspectRatio;
uniform float iTime;
uniform float Opacity;
uniform sampler2D NoiseTexture;
uniform float glitchAmplitude; // increase this, default: 0.2
uniform float glitchBlockiness;// default: 2.0

out vec4 OutColor;

float noise(vec2 uv, float blockiness)
{
    float timeOffset = floor(iTime * 20.0) / 10.0;
    return texture(NoiseTexture, uv * 0.1 + vec2(timeOffset * 0.1, 0.0)).r;
}

float fbm(vec2 uv, int count, float blockiness, float complexity)
{
    float val = 0.0;
    float amp = 0.5;

    while(count != 0)
    {
    	val += amp * noise(uv, blockiness);
        amp *= 0.5;
        uv *= complexity;
        count--;
    }

    return val;
}

const float glitchNarrowness = 4.0; // default: 4.0
const float glitchMinimizer = 6.0; // decrease this, default: 8.0

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = TexCoord;
    vec2 a = vec2(uv.x * AspectRatio, uv.y);
    vec2 uv2 = vec2(InvResolution, exp(a.y));

    float shift = glitchAmplitude * pow(fbm(uv2, 4, glitchBlockiness, glitchNarrowness), glitchMinimizer);

    float colR = texture(Source, vec2(uv.x + shift, uv.y)).r * (1. - shift);
    float colG = texture(Source, vec2(uv.x - shift, uv.y)).g * (1. - shift);
    float colB = texture(Source, vec2(uv.x - shift, uv.y)).b * (1. - shift);

    float colROpacity = texture(Source, vec2(uv.x + shift, uv.y)).a;
    float colGOpacity = texture(Source, vec2(uv.x - shift, uv.y)).a;
    float colBOpacity = texture(Source, vec2(uv.x - shift, uv.y)).a;

    // Mix with the scanline effect
    vec3 f = vec3(colR, colG, colB);
    float outOpacity = colROpacity * colGOpacity * colBOpacity * Opacity;
    if (outOpacity == 0.0) {
        discard;
    }

    fragColor = vec4(min(f / outOpacity, 1.0), outOpacity);
}

void main() {
    mainImage(OutColor, gl_FragCoord.xy);
}
