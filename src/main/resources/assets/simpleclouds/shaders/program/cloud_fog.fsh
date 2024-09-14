#version 330

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform float FogStart;
uniform float FogEnd;
uniform vec3 DefaultFogColor;

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
	vec4 col = texture(DiffuseSampler, texCoord);
	vec3 pos = screenToWorldPos(texCoord, texture(DiffuseDepthSampler, texCoord).x * 2.0 - 1.0);
	float depth = length(pos.xz);
	
	if (col.a > 0.0)
	{
		float fogFactor = 1.0 - min(max(depth - FogStart, 0.0) / (FogEnd - FogStart), 1.0);
		fragColor = vec4(col.rgb, col.a * fogFactor);
	}
	else
	{
		fragColor = vec4(0.0);
	}
}
