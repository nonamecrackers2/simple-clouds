#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D StormFogSampler;
uniform sampler2D CloudDepthSampler;
uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform float FogStart;
uniform float FogEnd;
uniform vec3 FogColor;
uniform int FogShape;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

float fogDistance(vec3 pos, int shape) 
{
    if (shape == 0) 
    {
        return length(pos);
    } 
    else 
    {
        float distXZ = length(pos.xz);
        float distY = length(pos.y);
        return max(distXZ, distY);
    }
}

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
	float screenDepth = texture(DiffuseDepthSampler, texCoord).x;
	vec4 col = texture(DiffuseSampler, texCoord);
	if (screenDepth >= texture(CloudDepthSampler, texCoord).x)
	{
		fragColor = vec4(col.rgb, 1.0);
		return;
	}
	
	vec3 pos = screenToWorldPos(texCoord, screenDepth * 2.0 - 1.0);
	
	float fogDist = fogDistance(pos, FogShape);
	
	float fogValue = fogDist < FogEnd ? smoothstep(FogStart, FogEnd, fogDist) : 1.0;
	if (fogDist <= FogStart)
		fogValue = 0.0;
		
	vec4 stormFogCol = texture(StormFogSampler, texCoord);
	vec3 fogCol = mix(FogColor, stormFogCol.rgb, stormFogCol.a);
	
	fragColor = vec4(mix(col.rgb, fogCol, fogValue), 1.0);
}
