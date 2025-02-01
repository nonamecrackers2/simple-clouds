#version 430

uniform sampler2D BayerMatrixSampler;

uniform vec4 ColorModulator;
uniform float DitherScale;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in vec4 vertexColor;
in float fogDistance;

out vec4 fragColor;

void main() 
{
	vec4 finalCol = ColorModulator * vertexColor;
	finalCol = mix(finalCol, FogColor, smoothstep(FogStart, FogEnd, fogDistance));
	
	float r = texture(BayerMatrixSampler, gl_FragCoord.xy * DitherScale).r;
	if (finalCol.a < r)
		discard;
    fragColor = vec4(finalCol.rgb, 1.0);
}
