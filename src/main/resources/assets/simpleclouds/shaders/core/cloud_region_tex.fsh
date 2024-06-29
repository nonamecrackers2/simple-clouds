#version 330

uniform sampler3D TexRegionSampler;
uniform vec4 ColorModulator;
uniform int LodLevel;
uniform int TotalCloudTypes;

in vec2 texCoord0;

out vec4 fragColor;

// https://gist.github.com/983/e170a24ae8eba2cd174f
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() 
{
    vec3 info = texture(TexRegionSampler, vec3(texCoord0, float(LodLevel) / 4.0)).rgb;
    float id = info.r;
    vec3 col = hsv2rgb(vec3(id / float(TotalCloudTypes), 1.0, 1.0));
    fragColor = vec4(col * info.g, 1.0);
}
