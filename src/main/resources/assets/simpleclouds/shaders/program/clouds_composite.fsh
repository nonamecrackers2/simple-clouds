#version 430

#define EPSILON 0.00001

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudsTexture;
uniform sampler2D AccumTexture;
uniform sampler2D RevealageTexture;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

float max4(vec4 col)
{
	return max(max(max(col.r, col.g), col.b), col.a);
}

void main() 
{
	vec4 cloudCol = texture(CloudsTexture, texCoord);
	vec3 bg = texture(DiffuseSampler, texCoord).rgb;
	vec3 finalCol = bg;
	finalCol = vec3(cloudCol.rgb * cloudCol.a + finalCol * (1.0 - cloudCol.a));

	// https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html
	ivec2 uv = ivec2(gl_FragCoord.xy);
	float revealage = texelFetch(RevealageTexture, uv, 0).r;
	if (revealage == 1.0)
	{
		fragColor = vec4(finalCol, 1.0);
		return;
	}
		
	vec4 accum = texelFetch(AccumTexture, uv, 0);
	if (isinf(max4(abs(accum))))
		accum.rgb = vec3(accum.a);
		
	vec3 avg = accum.rgb / max(accum.a, EPSILON);
	
	finalCol = vec3(avg * (1.0 - revealage) + finalCol * revealage);
	
	fragColor = vec4(finalCol, 1.0);
}
