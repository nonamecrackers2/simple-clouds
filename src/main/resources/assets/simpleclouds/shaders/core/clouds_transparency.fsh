//https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html

#version 430

uniform sampler2D BayerMatrixSampler;

uniform vec4 ColorModulator;
uniform float DitherScale;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in vec4 vertexColor;
in float fogDistance;
in float vertexDistance;

layout(location = 0) out vec4 accumColor;
layout(location = 1) out float revealage;

void main() 
{
	float fade = ColorModulator.a;
	float r = texture(BayerMatrixSampler, gl_FragCoord.xy * DitherScale).r;
	if (fade < r)
		discard;
	
	vec4 color = vertexColor * vec4(ColorModulator.rgb, 1.0);
	color = mix(color, FogColor, smoothstep(FogStart, FogEnd, fogDistance));
	
	vec4 premul = vec4(color.r * color.a, color.g * color.a, color.b * color.a, color.a);

	float z = min(vertexDistance / 1000.0, 1.0);
	float weight = max(premul.a * 3000.0 * pow(1.0 - z, 3.0), 0.01);
	
    accumColor = premul * weight;
    revealage = premul.a;
}
