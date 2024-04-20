layout(binding = 3, std140) uniform NoiseSettings
{
	float Height;
	float Threshold;
	float FadeThreshold;
	float ScaleX;
	float ScaleY;
	float ScaleZ;	
};
uniform vec3 Scroll;

float getNoiseAt(float x, float y, float z)
{
	float noise = snoise(vec3(x / ScaleX, y / ScaleY, z / ScaleZ) + Scroll);
	noise *= clamp(y / 10.0, 0.0, 1.0);
	return noise * clamp((Height - y) / 10.0, 0.0, 1.0);
}

bool isPosValid(float x, float y, float z)
{
	return getNoiseAt(x, y, z) > Threshold;
}