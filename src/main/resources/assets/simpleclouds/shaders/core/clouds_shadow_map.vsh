#version 150

in vec3 Position;
in float Darkness;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out float height;

void main() 
{
	height = Position.y;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	vertexColor = vec4(vec3(Darkness), 1.0);
}
