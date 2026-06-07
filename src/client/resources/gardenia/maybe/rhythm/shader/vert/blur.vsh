#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec2 TexCoord;

#import <coord_util.glsl>

void main() {
    FragCoord = rvertexcoord(gl_VertexID);

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    TexCoord = gl_Position.xy * 0.5 + 0.5;
}