#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler;

out vec4 OutColor;

void main() {
    vec4 color = texture(Sampler, TexCoord);

    if (color.a < 0.005) {
        discard;
    }

    OutColor = color * FragColor;
}