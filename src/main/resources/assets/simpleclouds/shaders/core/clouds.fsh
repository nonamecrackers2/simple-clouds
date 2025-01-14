#version 430

uniform sampler2D BayerMatrixSampler;

uniform vec4 ColorModulator;
uniform float DitherScale;

in vec4 vertexColor;

out vec4 fragColor;

void main() 
{
	vec4 finalCol = ColorModulator * vertexColor;
	float r = texture(BayerMatrixSampler, gl_FragCoord.xy * DitherScale).r;
	if (finalCol.a < r)
		discard;
    fragColor = vec4(finalCol.rgb, 1.0);
}
