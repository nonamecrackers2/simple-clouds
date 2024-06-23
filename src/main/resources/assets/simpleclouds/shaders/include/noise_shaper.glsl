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

uniform vec3 Scroll;

float getNoiseForLayer(NoiseLayer layer, float x, float y, float z)
{
	if (y < layer.HeightOffset || y > layer.HeightOffset + layer.Height)
		return -1000.0F;
	float noise = snoise(vec3(x / layer.ScaleX, y / layer.ScaleY, z / layer.ScaleZ) + Scroll) * layer.ValueScale + layer.ValueOffset;
	noise *= clamp((y - layer.HeightOffset) / layer.FadeDistance, 0.0, 1.0);
	noise *= clamp((layer.Height - (y - layer.HeightOffset)) / layer.FadeDistance, 0.0, 1.0);
	return noise;
}