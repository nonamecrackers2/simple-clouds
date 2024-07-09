#version 430

#moj_import <simpleclouds:random.glsl>

#define LOCAL_SIZE vec3(${LOCAL_SIZE_X}, ${LOCAL_SIZE_Y}, ${LOCAL_SIZE_Z})
layout(local_size_x = ${LOCAL_SIZE_X}, local_size_y = ${LOCAL_SIZE_Y}, local_size_z = ${LOCAL_SIZE_Z}) in;

//layout(rgba32f, binding = 0) uniform image2D mainImage;

layout(std430) restrict buffer LodScales {
	float data[];
}
lodScales;

layout(rg32f) uniform image3D mainImage;

uniform vec2 Scroll;
uniform float Scale = 10.0;
uniform float Spread = 1.0;
uniform int TotalCloudTypes = 1;
uniform vec2 Offset;

//vec2 cloudRegionIndexWithDist(vec2 pos) 
//{
//    vec2 indexUv = pos - mod(pos, Scale);
//	vec2 fractUv = mod(pos, Scale);
//	float minimumDist = Scale;  
//	vec2 minimumPoint;
//	for (int y = -1; y <= 1; y++) 
//	{
//		for (int x = -1; x <= 1; x++) 
//		{
//			vec2 neighbor = vec2(float(x) * Scale, float(y) * Scale);
//            vec2 point = vec2(random_vec2(indexUv + neighbor) * Scale * Spread);
//			vec2 diff = neighbor + point - fractUv;
//			float dist = length(diff);
//			if (dist < minimumDist) 
//			{
//				minimumDist = dist;
//				minimumPoint = point;
//			}
//		}
//	}
//	return vec2(floor(random_vec2(minimumPoint) * float(TotalCloudTypes)), minimumDist / Scale);
//}

//https://godotshaders.com/snippet/voronoi/
//https://www.shadertoy.com/view/llG3zy
vec2 cloudRegionIndexWithBoundaryFade(vec2 pos) 
{
    vec2 indexUv = floor(pos);
	vec2 fractUv = fract(pos);

	float minimumDist = 8.0;  
    vec2 closestCoord;
    vec2 closestPoint;
	for (float y = -1.0; y <= 1.0; y++) 
	{
		for (float x = -1.0; x <= 1.0; x++) 
		{
			vec2 neighbor = vec2(x, y);
            vec2 point = vec2(random_vec2(indexUv + neighbor) * Spread);
			vec2 coord = neighbor + point - fractUv;
			float dist = length(coord);
			if (dist < minimumDist) 
			{
				minimumDist = dist;
                closestCoord = coord;
                closestPoint = point;
			}
		}
	}
    minimumDist = 8.0;
    for (float y = -1.0; y <= 1.0; y++) 
	{
		for (float x = -1.0; x <= 1.0; x++) 
		{
			vec2 neighbor = vec2(x, y);
            vec2 point = vec2(random_vec2(indexUv + neighbor) * Spread);
			vec2 coord = neighbor + point - fractUv;
			if (length(closestCoord - coord) > 0.0)
                minimumDist = min(minimumDist, dot(0.5 * (closestCoord + coord), normalize(coord - closestCoord)));
		}
	}
	return vec2(floor(random_vec2(closestPoint) * float(TotalCloudTypes)), 1.0 - min(minimumDist * 3.0, 1.0));
}

// https://gist.github.com/983/e170a24ae8eba2cd174f
//vec3 hsv2rgb(vec3 c)
//{
//    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
//    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
//    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
//}

void main()
{
	uint lod = gl_GlobalInvocationID.z;
	float scale = lodScales.data[lod];

	vec2 centerOffset = vec2(imageSize(mainImage).xy) / 2.0;
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    vec2 uv = ((vec2(gl_GlobalInvocationID.xy) - centerOffset) * scale + Scroll + Offset) / Scale;
	vec2 info = cloudRegionIndexWithBoundaryFade(uv);
	//vec3 col = hsv2rgb(vec3(float(id) / float(TotalCloudTypes), 1.0, 1.0));
    imageStore(mainImage, ivec3(texelCoord, lod), vec4(info, 0.0, 0.0));//vec4(col, 1.0));
}