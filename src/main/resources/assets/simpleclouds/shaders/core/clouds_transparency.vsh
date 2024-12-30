#version 430

#define SHADE vec3(0.6, 0.7, 0.8)

in vec3 Position;
in float Brightness;
in float Alpha;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 DarknessColorModifier;

out vec4 vertexColor;

void main() 
{
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	vertexColor = vec4(mix(DarknessColorModifier, vec3(1.0), Brightness), Alpha);
}
