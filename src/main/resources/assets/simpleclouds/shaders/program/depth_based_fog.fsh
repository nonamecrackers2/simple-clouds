#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 WorldProjMat;
uniform mat4 ModelViewMat;
uniform vec2 ScreenSize;
uniform float FogStart;
uniform float FogEnd;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

vec3 screenToWorldPos(vec2 coord, float depth)
{
	vec3 ndc = vec3(coord * 2.0 - 1.0, depth);
  	vec4 view = inverse(WorldProjMat) * vec4(ndc, 1.0);
  	view.xyz /= view.w;
  	vec3 result = (inverse(ModelViewMat) * view).xyz;
  	return result;
}

void main() 
{
	vec4 col = texture(DiffuseSampler, texCoord);
	if (col.a > 0.0)
	{
		vec3 pos = screenToWorldPos(texCoord, texture(DiffuseDepthSampler, texCoord).x * 2.0 - 1.0);
		float depth = length(pos);
		float fogFactor = 1.0 - min(max(depth - FogStart, 0.0) / (FogEnd - FogStart), 1.0);
		fragColor = vec4(col.rgb, fogFactor);
	}
}
