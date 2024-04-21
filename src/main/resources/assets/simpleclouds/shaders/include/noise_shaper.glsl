struct NoiseLayer {
	float Height;
	float ValueOffset;
	float ScaleX;
	float ScaleY;
	float ScaleZ;
	float FadeDistance;
	float HeightOffset;
	float ValueScale;
};

layout(binding = 3, std430) readonly buffer NoiseLayers {
	NoiseLayer data[];
}
layers;
uniform int LayerCount;
uniform vec3 Scroll;

float getNoiseForLayer(NoiseLayer layer, float x, float y, float z)
{
	y -= layer.HeightOffset;
	float noise = snoise(vec3(x / layer.ScaleX, y / layer.ScaleY, z / layer.ScaleZ) + Scroll);
	noise *= clamp((y - layer.HeightOffset) / layer.FadeDistance, 0.0, 1.0);
	return noise * clamp((layer.Height - (y - layer.HeightOffset)) / layer.FadeDistance, 0.0, 1.0) * layer.ValueScale + layer.ValueOffset;
}

float getNoiseAt(float x, float y, float z)
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

bool isPosValid(float x, float y, float z)
{
	return getNoiseAt(x, y, z) > 0.0;
}