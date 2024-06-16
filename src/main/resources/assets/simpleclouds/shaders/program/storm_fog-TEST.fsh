#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMap;
uniform mat4 WorldProjMat;
uniform mat4 ModelViewMat;
uniform mat4 ShadowProjMat;
uniform mat4 ShadowModelViewMat;
uniform vec3 CameraPos;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

vec3 mainScreenToWorldPos(vec2 coord, float depth)
{
	vec3 ndc = vec3(coord * 2.0 - 1.0, depth);
  	vec4 view = inverse(WorldProjMat) * vec4(ndc, 1.0);
  	view.xyz /= view.w;
  	vec3 result = (inverse(ModelViewMat) * view).xyz;
  	return result;
}

float calculateShadow(vec3 pos)
{
	vec4 shadowMapPos = ShadowProjMat * ShadowModelViewMat * vec4(pos, 1.0);
	vec3 ndc = shadowMapPos.xyz / shadowMapPos.w;
	vec3 coord = ndc * 0.5 + 0.5;
	float shadowMapDepth = texture(ShadowMap, coord.xy).x;
	return shadowMapDepth > coord.z ? 1.0 : 0.7;
}

void main() 
{
	vec3 worldPos = mainScreenToWorldPos(texCoord, texture(DiffuseDepthSampler, texCoord).x * 2.0 - 1.0);
	if (length(worldPos) < 1000.0)
	{
		float shadowStrength = calculateShadow(worldPos + CameraPos);
		vec3 originalColor = texture(DiffuseSampler, texCoord).rgb;
		fragColor = vec4(originalColor * shadowStrength, 1.0);
	}
	else
	{
		fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
	}
}
