#version 430

#define EPSILON 0.00001

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudsTexture;
uniform sampler2D CloudsDepthTexture;
uniform sampler2D AccumTexture;
uniform sampler2D RevealageTexture;

uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform float FogStart;
uniform float FogEnd;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

vec3 screenToWorldPos(vec2 coord, float depth)
{
	vec3 ndc = vec3(coord * 2.0 - 1.0, depth);
  	vec4 view = InverseWorldProjMat * vec4(ndc, 1.0);
  	view.xyz /= view.w;
  	vec3 result = (InverseModelViewMat * view).xyz;
  	return result;
}

float max4(vec4 col)
{
	return max(max(max(col.r, col.g), col.b), col.a);
}

void main() 
{
	vec4 cloudCol = texture(CloudsTexture, texCoord);
	float cloudDepth = length(screenToWorldPos(texCoord, texture(CloudsDepthTexture, texCoord).x * 2.0 - 1.0));
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
