#deprecated

#version 430

struct Lightning {
	vec3 Position;
	float Alpha;
};

layout(std430) readonly buffer LightningBolts {
	Lightning data[];
}
lightning;

uniform int TotalLightningBolts;

uniform sampler2D ShadowMap;
uniform sampler2D ShadowMapColor;
uniform sampler2D DepthSampler;
uniform sampler2D DiffuseSampler;
uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform mat4 ShadowProjMat;
uniform mat4 ShadowModelViewMat;
uniform vec3 CameraPos;
uniform vec3 ColorThreshold;
uniform vec3 ColorMultiplier;
uniform vec4 ColorModulator;

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

vec4 shadowMapColorAt(vec3 pos)
{
	vec4 shadowMapPos = ShadowProjMat * ShadowModelViewMat * vec4(pos, 1.0);
	vec3 ndc = shadowMapPos.xyz / shadowMapPos.w;
	vec3 coord = ndc * 0.5 + 0.5;
	float shadowMapDepth = texture(ShadowMap, coord.xy).x;
	if (shadowMapDepth < 1.0 && shadowMapDepth < coord.z)
		return vec4(texture(ShadowMapColor, coord.xy).rgb, 1.0);
	return vec4(0.0);
}

float getNearestLightningBoltColorModifier(vec3 position)
{
	for (int i = 0; i < TotalLightningBolts; i++)
	{
		Lightning bolt = lightning.data[i];
		float dist = distance(bolt.Position.xz, position.xz);
		if (dist < 2000.0)
		{
			float distMul = clamp(2.0 - dist * 0.001, 0.0, 1.0);
			return 1.0 + bolt.Alpha * distMul;
		}
	}
	return 1.0;
}

void main() 
{
	vec3 pos = screenToWorldPos(texCoord, texture(DepthSampler, texCoord).r * 2.0 - 1.0);
	vec3 col = texture(DiffuseSampler, texCoord).rgb;
	if (length(pos) < 5000.0)
	{
		vec4 shadowMapCol = shadowMapColorAt(pos + CameraPos);
		if (shadowMapCol.a > 0.0 && shadowMapCol.r <= ColorThreshold.r && shadowMapCol.g <= ColorThreshold.g && shadowMapCol.b <= ColorThreshold.b)
    	{
    		vec3 stormColor = shadowMapCol.rgb * ColorMultiplier * ColorModulator.rgb;
    		float alpha = smoothstep(0.0, 800.0, length(pos.xz));
    		vec3 finalCol = mix(col, stormColor, alpha);
			fragColor = vec4(finalCol, 1.0);
		}
		else
		{
			fragColor = vec4(col, 1.0);
		}
	}
	else
	{
		fragColor = vec4(col, 1.0);
	}
}
