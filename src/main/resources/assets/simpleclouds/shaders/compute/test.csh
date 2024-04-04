#version 430

#moj_import <simpleclouds:noise.glsl>

struct Vertex {
	float x;
	float y;
	float z;
	float r;
	float g;
	float b;
	float a;
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

//Render params
uniform vec2 RenderOffset;

//Noise params
uniform float Threshold = 0.5;
uniform vec3 NoiseScale = vec3(30.0);
uniform float CloudHeight = 32.0;
uniform vec3 Scroll;

bool isPosValid(float x, float y, float z)
{
	float noise = snoise(vec3(x, y, z) / NoiseScale + Scroll);
	noise *= clamp(y / 10.0, 0.0, 1.0);
	noise *= clamp((CloudHeight - y) / 10.0, 0.0, 1.0);
	return noise > Threshold;
}

void createFace(vec3 offset, vec3 corner1, vec3 corner2, vec3 corner3, vec3 corner4, vec3 normal)
{
	uint currentFace = atomicCounterIncrement(counter);
	uint lastIndex = currentFace * 6;
	uint lastVertex = currentFace * 4;
	Side side;
	side.a = Vertex(offset.x + corner1.x, offset.y + corner1.y, offset.z + corner1.z, 1.0, 1.0, 1.0, 1.0, normal.x, normal.y, normal.z);
	side.b = Vertex(offset.x + corner2.x, offset.y + corner2.y, offset.z + corner2.z, 1.0, 1.0, 1.0, 1.0, normal.x, normal.y, normal.z);
	side.c = Vertex(offset.x + corner3.x, offset.y + corner3.y, offset.z + corner3.z, 1.0, 1.0, 1.0, 1.0, normal.x, normal.y, normal.z);
	side.d = Vertex(offset.x + corner4.x, offset.y + corner4.y, offset.z + corner4.z, 1.0, 1.0, 1.0, 1.0, normal.x, normal.y, normal.z);
	sides.data[currentFace] = side;
	for (uint i = 0; i < sideIndices.length; i++)
		indices.data[lastIndex + i] = lastVertex + sideIndices[i];
}

void createFaceInvert(vec3 offset, vec3 corner1, vec3 corner2, vec3 corner3, vec3 corner4, vec3 normal)
{
	createFace(offset, corner4, corner3, corner2, corner1, normal);
}

void createCube(float x, float y, float z)
{
	vec3 offset = vec3(x + 0.5, y + 0.5, z + 0.5);
	//-Y
	if (!isPosValid(x, y - 1.0, z))
		createFace(offset, vec3(-0.5, -0.5, -0.5), vec3(0.5, -0.5, -0.5), vec3(0.5, -0.5, 0.5), vec3(-0.5, -0.5, 0.5), vec3(0.0, -1.0, 0.0));
	//+Y
	if (!isPosValid(x, y + 1.0, z))
		createFaceInvert(offset, vec3(-0.5, 0.5, -0.5), vec3(0.5, 0.5, -0.5), vec3(0.5, 0.5, 0.5), vec3(-0.5, 0.5, 0.5), vec3(0.0, 1.0, 0.0));
	//+X
	if (!isPosValid(x - 1.0, y, z))
		createFaceInvert(offset, vec3(-0.5, -0.5, -0.5), vec3(-0.5, 0.5, -0.5), vec3(-0.5, 0.5, 0.5), vec3(-0.5, -0.5, 0.5), vec3(-1.0, 0.0, 0.0));
	//+X
	if (!isPosValid(x + 1.0, y, z))
		createFace(offset, vec3(0.5, -0.5, -0.5), vec3(0.5, 0.5, -0.5), vec3(0.5, 0.5, 0.5), vec3(0.5, -0.5, 0.5), vec3(1.0, 0.0, 0.0));
	//-Z
	if (!isPosValid(x, y, z - 1.0))
		createFace(offset, vec3(-0.5, -0.5, -0.5), vec3(-0.5, 0.5, -0.5), vec3(0.5, 0.5, -0.5), vec3(0.5, -0.5, -0.5), vec3(0.0, 0.0, -1.0));
	//+Z
	if (!isPosValid(x, y, z + 1.0))
		createFaceInvert(offset, vec3(-0.5, -0.5, 0.5), vec3(-0.5, 0.5, 0.5), vec3(0.5, 0.5, 0.5), vec3(0.5, -0.5, 0.5), vec3(0.0, 0.0, 1.0));
}

void main() 
{
    vec3 id = gl_GlobalInvocationID;
    float x = id.x + RenderOffset.x;
    float y = id.y;
    float z = id.z + RenderOffset.y;
    
    if (isPosValid(x, y, z))
	    createCube(x, y, z);
}