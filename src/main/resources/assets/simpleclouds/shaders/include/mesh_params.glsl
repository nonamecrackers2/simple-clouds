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

struct Vertex {
	float x;
	float y;
	float z;
	float brightness;
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

layout(std430) coherent buffer Counter {
	uint counter;
};

layout(std430) restrict buffer SideDataBuffer {
    Side data[];
}
sides;

layout(std430) restrict buffer IndexBuffer {
	uint data[];
}
indices;

layout(std430) readonly buffer NoiseLayers {
	NoiseLayer data[];
}
layers;

//Render params
uniform int LodLevel;
uniform vec3 RenderOffset;
uniform float Scale = 1.0;
uniform bool AddMovementSmoothing;
uniform vec3 Scroll;
uniform int LayerCount;
uniform float FadeStart;
uniform float FadeEnd;
uniform vec3 Origin;

//Faces:
//-X = 0
//+X = 1
//-Y = 2
//+Y = 3
//-Z = 4
//+Z = 5

uniform int DoNotOccludeSide = -1;