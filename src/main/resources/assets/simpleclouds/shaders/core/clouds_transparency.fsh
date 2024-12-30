#version 430

uniform vec4 ColorModulator;

in vec4 vertexColor;

layout(location = 0) out vec4 accumColor;
layout(location = 1) out float revealage;

void main() 
{
	vec4 color = ColorModulator * vertexColor;
	vec4 premul = vec4(color.r * color.a, color.g * color.a, color.b * color.a, color.a);

	float weight = premul.a * max(0.1, 10000.0 * pow((1.0 - gl_FragCoord.z), 3.0));
	
    accumColor = premul * weight;
    revealage = premul.a;
}
