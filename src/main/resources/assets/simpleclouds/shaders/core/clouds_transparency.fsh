//https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html

#version 430

uniform vec4 ColorModulator;

in vec4 vertexColor;

layout(location = 0) out vec4 accumColor;
layout(location = 1) out float revealage;

void main() 
{
	vec4 color = ColorModulator * vertexColor;
	vec4 premul = vec4(color.r * color.a, color.g * color.a, color.b * color.a, color.a);

	float weight = premul.a * max(0.1, 1000.0 * pow((1.0 - gl_FragCoord.z), 3.0));
	
    accumColor = premul * weight;
    revealage = premul.a;
}
