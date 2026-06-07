#version 150

in vec3 Position;

uniform vec2 Resolution;

out vec2 FragCoord;
out vec2 TexCoord;

const vec2[4] QUAD_COORDS = vec2[] (
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

void main() {
    gl_Position = vec4(
        (Position.x / Resolution.x) * 2.0 - 1.0,
        1.0 - (Position.y / Resolution.y) * 2.0,
        0.0,
        1.0
    );

    FragCoord = QUAD_COORDS[gl_VertexID % 4];
    TexCoord = Position.xy / Resolution;
}
