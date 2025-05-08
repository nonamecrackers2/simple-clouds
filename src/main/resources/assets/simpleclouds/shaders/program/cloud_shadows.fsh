#version 150

uniform sampler2DShadow ShadowMap;
uniform sampler2D DepthSampler;
uniform sampler2D DiffuseSampler;
uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform mat4 ShadowProjMat;
uniform mat4 ShadowModelViewMat;
uniform vec3 CameraPos;
uniform vec3 ShadowColorMultiplier;
uniform float ShadowSpan;
uniform float MinimumRadius;
uniform float FadeDistance;

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

float shadowStrengthAt(vec3 pos)
{
	vec4 shadowMapPos = ShadowProjMat * ShadowModelViewMat * vec4(pos, 1.0);
	vec3 ndc = shadowMapPos.xyz / shadowMapPos.w;
	vec3 coord = ndc * 0.5 + 0.5;
	return texture(ShadowMap, coord);
}

void main() 
{
	vec3 col = texture(DiffuseSampler, texCoord).rgb;
	
	vec3 localPos = screenToWorldPos(texCoord, texture(DepthSampler, texCoord).r * 2.0 - 1.0);
	vec3 pos = CameraPos + localPos;
	
	float len = length(localPos);
	float f = FadeDistance;
	float invF = 1.0 / f;
	float start = MinimumRadius + 32.0;
	float end = ShadowSpan / 2.0;
	
	if (len < start || len > end)
	{
		fragColor = vec4(col, 1.0);
		return;
	}
	
	float distFade = clamp(invF * (len - start), 0.0, 1.0) - clamp(invF * (len - end + f), 0.0, 1.0);
	
	//https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping
	float strength = 0.0;
	for (int x = -1; x <= 1; x++)
	{
		for (int y = -1; y <= 1; y++)
			strength += shadowStrengthAt(pos + vec3(x, 0.0, y) * 10.0);
	}
	strength /= 9.0;
	strength *= distFade;
	strength = clamp(strength, 0.0, 1.0);
	
	vec3 shadowCol = col * ShadowColorMultiplier;
	vec3 finalCol = mix(col, shadowCol, strength);
	fragColor = vec4(finalCol, 1.0);
}
