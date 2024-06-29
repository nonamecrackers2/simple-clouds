float getNoiseForLayer(NoiseLayer layer, float x, float y, float z)
{
	if (y < layer.HeightOffset || y > layer.HeightOffset + layer.Height)
		return 0.0;
	float noise = snoise((vec3(x, y, z) + Scroll) / vec3(layer.ScaleX, layer.ScaleY, layer.ScaleZ)) * layer.ValueScale + layer.ValueOffset;
	noise -= 1.0 - clamp((y - layer.HeightOffset) / layer.FadeDistance, 0.0, 1.0);
	noise -= 1.0 - clamp((layer.Height - (y - layer.HeightOffset)) / layer.FadeDistance, 0.0, 1.0);
	return noise;
}