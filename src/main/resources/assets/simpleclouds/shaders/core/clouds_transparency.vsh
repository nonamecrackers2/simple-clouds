#version 430

in vec3 Position;

struct TransparentCubeInfo {
	float x;
	float y;
	float z;
	float brightness;
	float alpha;
	float radius;
};

layout(std430) restrict readonly buffer TransparentCubeInfoBuffer {
    TransparentCubeInfo data[];
}
cubesTransparent;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 DarknessColorModifier;

out vec4 vertexColor;
out float fogDistance;
out float vertexDistance;

void main() 
{
	TransparentCubeInfo info = cubesTransparent.data[gl_InstanceID];
	vec3 cubeOffset = vec3(info.x, info.y, info.z);
	vec4 finalPos = vec4(Position * info.radius + cubeOffset, 1.0);
    gl_Position = ProjMat * ModelViewMat * finalPos;
	vertexColor = vec4(mix(DarknessColorModifier, vec3(1.0), info.brightness), info.alpha);
	vec4 modelPos = ModelViewMat * finalPos;
	fogDistance = length(modelPos.xz);
	vertexDistance = length(modelPos.xyz);
}