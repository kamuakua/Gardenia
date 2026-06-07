#version 150

in vec2 FragCoord;
in vec4 FragColor;

out vec4 OutColor;

void main() {
    OutColor = FragColor;
}