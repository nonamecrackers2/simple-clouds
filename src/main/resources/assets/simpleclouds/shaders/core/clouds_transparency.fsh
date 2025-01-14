//https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html

#version 430

uniform sampler2D BayerMatrixSampler;

uniform vec4 ColorModulator;
uniform float DitherScale;
uniform float FogStart;
uniform float FogEnd;

in vec4 vertexColor;
in float vertexDistance;

layout(location = 0) out vec4 accumColor;
layout(location = 1) out float revealage;

void main() 
{
	float fade = ColorModulator.a;
	float r = texture(BayerMatrixSampler, gl_FragCoord.xy * DitherScale).r;
	if (fade < r)
		discard;
	
	float fogFactor = 1.0 - min(max(vertexDistance - FogStart, 0.0) / (FogEnd - FogStart), 1.0);
	vec4 color = vec4(ColorModulator.rgb, fogFactor) * vertexColor;
	vec4 premul = vec4(color.r * color.a, color.g * color.a, color.b * color.a, color.a);

	float weight = premul.a * max(0.1, 1000.0 * pow((1.0 - gl_FragCoord.z), 3.0));
	
    accumColor = premul * weight;
    revealage = premul.a;
}
