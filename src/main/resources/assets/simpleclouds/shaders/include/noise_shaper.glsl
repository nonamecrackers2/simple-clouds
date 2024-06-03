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
	y -= layer.HeightOffset;
	float noise = snoise(vec3(x / layer.ScaleX, y / layer.ScaleY, z / layer.ScaleZ) + Scroll);
	noise *= clamp((y - layer.HeightOffset) / layer.FadeDistance, 0.0, 1.0);
	return noise * clamp((layer.Height - (y - layer.HeightOffset)) / layer.FadeDistance, 0.0, 1.0) * layer.ValueScale + layer.ValueOffset;
}