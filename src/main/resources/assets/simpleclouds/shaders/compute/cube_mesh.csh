#version 430

#moj_import <simpleclouds:simplex_noise.glsl>
#moj_import <simpleclouds:noise_shaper.glsl>

#define LOCAL_SIZE vec3(${LOCAL_SIZE_X}, ${LOCAL_SIZE_Y}, ${LOCAL_SIZE_Z})

struct LayerGroup {
	int StartIndex;
	int EndIndex;
};

struct Vertex {
	float x;
	float y;
	float z;
//	float r;
//	float g;
//	float b;
//	float a;
	float nx;
	float ny;
	float nz;
};

struct Side {
	Vertex a;
	Vertex b;
	Vertex c;
	Vertex d;
};

const uint sideIndices[6] = {
	0, 1, 2, 0, 2, 3
};

layout(local_size_x = ${LOCAL_SIZE_X}, local_size_y = ${LOCAL_SIZE_Y}, local_size_z = ${LOCAL_SIZE_Z}) in;

layout(binding = 0) uniform atomic_uint counter;

layout(binding = 1, std430) restrict buffer SideDataBuffer {
    Side data[];
}
sides;

layout(binding = 2, std430) restrict buffer IndexBuffer {
	uint data[];
}
indices;

layout(binding = 3, std430) readonly buffer NoiseLayers {
	NoiseLayer data[];
}
layers;

layout(binding = 4, std430) readonly buffer LayerGroupings {
	LayerGroup data[];
}
layerGroupings;

layout(rg8, binding = 0) uniform image3D regions;

//Render params
uniform int LodLevel;
uniform vec3 RenderOffset;
uniform vec2 RegionSampleOffset;
uniform float Scale = 1.0;
uniform bool AddMovementSmoothing;

//Faces:
//-X = 0
//+X = 1
//-Y = 2
//+Y = 3
//-Z = 4
//+Z = 5

uniform int DoNotOccludeSide = -1;

bool shouldNotOcclude(int index)
{
	if (DoNotOccludeSide != -1 && index == DoNotOccludeSide)
	{
		vec3 id = gl_GlobalInvocationID;
		vec3 size = gl_NumWorkGroups * LOCAL_SIZE;
		if (DoNotOccludeSide == 1)
			return id.x == size.x - 1.0;
		else if (DoNotOccludeSide == 0)
			return id.x == 0.0;
		else if (DoNotOccludeSide == 3)
			return id.y == size.y - 1.0;
		else if (DoNotOccludeSide == 2)
			return id.y == 0.0;
		else if (DoNotOccludeSide == 5)
			return id.z == size.z - 1.0;
		else if (DoNotOccludeSide == 4)
			return id.z == 0.0;
		else
			return false;			
	}
	else
	{
		return false;
	}
}

void createFace(vec3 offset, vec3 corner1, vec3 corner2, vec3 corner3, vec3 corner4, vec3 normal)
{
	uint currentFace = atomicCounterIncrement(counter);
	uint lastIndex = currentFace * 6;
	uint lastVertex = currentFace * 4;
	Side side;
	side.a = Vertex(offset.x + corner1.x, offset.y + corner1.y, offset.z + corner1.z, normal.x, normal.y, normal.z);
	side.b = Vertex(offset.x + corner2.x, offset.y + corner2.y, offset.z + corner2.z, normal.x, normal.y, normal.z);
	side.c = Vertex(offset.x + corner3.x, offset.y + corner3.y, offset.z + corner3.z, normal.x, normal.y, normal.z);
	side.d = Vertex(offset.x + corner4.x, offset.y + corner4.y, offset.z + corner4.z, normal.x, normal.y, normal.z);
	sides.data[currentFace] = side;
	for (uint i = 0; i < sideIndices.length; i++)
		indices.data[lastIndex + i] = lastVertex + sideIndices[i];
}

void createFaceInvert(vec3 offset, vec3 corner1, vec3 corner2, vec3 corner3, vec3 corner4, vec3 normal)
{
	createFace(offset, corner4, corner3, corner2, corner1, normal);
}

float getNoiseForLayerGroup(LayerGroup group, float x, float y, float z)
{
	int totalLayers = group.EndIndex - group.StartIndex;
	if (totalLayers > 0)
	{
		float combinedNoise = getNoiseForLayer(layers.data[group.StartIndex], x, y, z);
		for (int i = 1; i < totalLayers; i++)
			combinedNoise += getNoiseForLayer(layers.data[i + group.StartIndex], x, y, z);
		return combinedNoise;
	}
	else
	{
		return 0.0F;
	}
}

bool isPosValid(float x, float y, float z, int nx, int ny, int nz)
{
	ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xz + RegionSampleOffset) + ivec2(nx, nz);
    vec4 info = imageLoad(regions, ivec3(texelCoord, LodLevel));
    uint regionId = uint(info.r);
    float fade = info.g - 0.2F;
    LayerGroup group = layerGroupings.data[regionId];
    return getNoiseForLayerGroup(group, x, y, z) + log(1.0 - fade) > 0.0F;
}

void createCube(float x, float y, float z, bool occlude, float cubeRadius)
{
	vec3 offset = vec3(x + cubeRadius, y + cubeRadius, z + cubeRadius);
	//-Y
	if (!occlude || !isPosValid(x, y - Scale, z, 0, -1, 0) || shouldNotOcclude(2))
		createFace(offset, vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(0.0, -1.0, 0.0));
	//+Y
	if (!occlude || !isPosValid(x, y + Scale, z, 0, 1, 0) || shouldNotOcclude(3))
		createFaceInvert(offset, vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(0.0, 1.0, 0.0));
	//-X
	if (!occlude || !isPosValid(x - Scale, y, z, -1, 0, 0) || shouldNotOcclude(0))
		createFaceInvert(offset, vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(-1.0, 0.0, 0.0));
	//+X
	if (!occlude || !isPosValid(x + Scale, y, z, 1, 0, 0) || shouldNotOcclude(1))
		createFace(offset, vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(1.0, 0.0, 0.0));
	//-Z
	if (!occlude || !isPosValid(x, y, z - Scale, 0, 0, -1) || shouldNotOcclude(4))
		createFace(offset, vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(0.0, 0.0, -1.0));
	//+Z
	if (!occlude || !isPosValid(x, y, z + Scale, 0, 0, 1) || shouldNotOcclude(5))
		createFaceInvert(offset, vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(0.0, 0.0, 1.0));
}

void main() 
{
    vec3 id = gl_GlobalInvocationID;
    float x = id.x * Scale + RenderOffset.x;
    float y = id.y * Scale + RenderOffset.y;
    float z = id.z * Scale + RenderOffset.z;
    
    if (isPosValid(x, y, z, 0, 0, 0))
    {
		createCube(x, y, z, true, Scale / 2.0);
    }
	//else if (AddMovementSmoothing)
	//{
	//	float noise = getNoiseAt(x, y, z);
	//	if (noise > Layer.FadeThreshold)
	//		createCube(x, y, z, false, (noise - Layer.FadeThreshold) / (Layer.Threshold - Layer.FadeThreshold) * 0.5);
	//}
}