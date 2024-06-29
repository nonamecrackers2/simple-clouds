#version 430

#define LOCAL_SIZE vec3(${LOCAL_SIZE_X}, ${LOCAL_SIZE_Y}, ${LOCAL_SIZE_Z})

layout(local_size_x = ${LOCAL_SIZE_X}, local_size_y = ${LOCAL_SIZE_Y}, local_size_z = ${LOCAL_SIZE_Z}) in;

#moj_import <simpleclouds:simplex_noise.glsl>
#moj_import <simpleclouds:mesh_params.glsl>
#moj_import <simpleclouds:mesh_noise.glsl>

uniform float Storminess;
uniform float StormStart;
uniform float StormFadeDistance;

float getNoiseForLayers(float x, float y, float z)
{
	if (LayerCount > 0)
	{
		float combinedNoise = getNoiseForLayer(layers.data[0], x, y, z);
		for (int i = 1; i < LayerCount; i++)
			combinedNoise += getNoiseForLayer(layers.data[i], x, y, z);
		return combinedNoise;
	}
	else
	{
		return 0.0F;
	}
}


float getBrightnessForPosition(float x, float y, float z, int nx, int ny, int nz)
{
	float len = length(vec3(x, y, z));
	float factor = 1.0 - min(max(len - FadeStart, 0.0) / (FadeEnd - FadeStart), 1.0);
    bool passedThresh = getNoiseForLayers(x, y, z) + log(factor) > 0.0;
    if (passedThresh)
    	return 1.0 - Storminess * (1.0 - clamp((y - StormStart) / StormFadeDistance, 0.0, 1.0));
    else
    	return -1.0;
}

#moj_import <simpleclouds:mesh_base.glsl>