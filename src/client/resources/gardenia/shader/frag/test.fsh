#version 150

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;

uniform sampler2D Sampler;
uniform vec2 Resolution;
uniform vec2 Size;
uniform vec2 TexelSize;
uniform vec2 Direction;

uniform float Opacity;
uniform float Strength;
uniform float Intensity;
uniform float Smoothness;
uniform float Brightness;
uniform vec4 Radius;

out vec4 OutColor;

#import <common.glsl>

vec4 blur(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
  vec4 color = vec4(0.0);
  vec2 off1 = vec2(1.411764705882353) * direction;
  vec2 off2 = vec2(3.2941176470588234) * direction;
  vec2 off3 = vec2(5.176470588235294) * direction;
  color += texture(image, uv) * 0.1964825501511404;
  color += texture(image, uv + (off1 / resolution)) * 0.2969069646728344;
  color += texture(image, uv - (off1 / resolution)) * 0.2969069646728344;
  color += texture(image, uv + (off2 / resolution)) * 0.09447039785044732;
  color += texture(image, uv - (off2 / resolution)) * 0.09447039785044732;
  color += texture(image, uv + (off3 / resolution)) * 0.010381362401148057;
  color += texture(image, uv - (off3 / resolution)) * 0.010381362401148057;
  return color;
}

void main() {
    float smoothedAlpha = smoothen(rsdf(Size, FragCoord, Radius), Smoothness);
    if (smoothedAlpha == 0.0) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec4 color = blur(Sampler, uv, Resolution, Direction);
    OutColor = vec4(color.rgb * Brightness, color.a);
}