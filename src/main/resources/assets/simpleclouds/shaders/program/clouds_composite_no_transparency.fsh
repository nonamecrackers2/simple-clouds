#version 430

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudsTexture;
uniform sampler2D CloudsDepthTexture;

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

void main() 
{
	vec4 cloudCol = texture(CloudsTexture, texCoord);
	float cloudDepth = length(screenToWorldPos(texCoord, texture(CloudsDepthTexture, texCoord).x * 2.0 - 1.0));
	vec3 bg = texture(DiffuseSampler, texCoord).rgb;
	vec3 finalCol = bg;
	finalCol = vec3(cloudCol.rgb * cloudCol.a + finalCol * (1.0 - cloudCol.a));
	fragColor = vec4(finalCol, 1.0);
}
