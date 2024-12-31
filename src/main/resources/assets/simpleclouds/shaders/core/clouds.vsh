#version 430

#define SHADE vec3(0.6, 0.7, 0.8)

in vec3 Position;

struct SideInfo {
	int side;
	float x;
	float y;
	float z;
	float brightness;
	float radius;
};

layout(std430) restrict readonly buffer SideInfoBuffer {
    SideInfo data[];
}
sides;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform float LightPower;
uniform float AmbientLight;
uniform vec3 DarknessColorModifier;

out vec4 vertexColor;

const vec3 normals[6] = {
	vec3(-1.0, 0.0, 0.0),
	vec3(1.0, 0.0, 0.0),
	vec3(0.0, -1.0, 0.0),
	vec3(0.0, 1.0, 0.0),
	vec3(0.0, 0.0, -1.0),
	vec3(0.0, 0.0, 1.0)
};

const mat4 transformations[6] = {
	mat4(
		1.0, 0.0, 0.0, 0.0,
		0.0, 1.0, 0.0, 0.0,
		0.0, 0.0, 1.0, 0.0,
		0.0, 0.0, 0.0, 1.0
	),
	mat4(
	   -1.0, 0.0, 0.0, 0.0,
		0.0, 1.0, 0.0, 0.0,
		0.0, 0.0, 1.0, 0.0,
		0.0, 0.0, 0.0, 1.0
	),
	mat4(
		0.0, -1.0, 0.0, 0.0,
		1.0,  0.0, 0.0, 0.0,
		0.0,  0.0, 1.0, 0.0,
		0.0,  0.0, 0.0, 1.0
	),
	mat4(
		0.0, 1.0, 0.0, 0.0,
	   -1.0, 0.0, 0.0, 0.0,
		0.0, 0.0, 1.0, 0.0,
		0.0, 0.0, 0.0, 1.0
	),
	mat4(
		0.0, 0.0, -1.0, 0.0,
		0.0, 1.0,  0.0, 0.0,
	    1.0, 0.0,  0.0, 0.0,
		0.0, 0.0,  0.0, 1.0
	),
	mat4(
		0.0, 0.0, 1.0, 0.0,
		0.0, 1.0, 0.0, 0.0,
	   -1.0, 0.0, 0.0, 0.0,
		0.0, 0.0, 0.0, 1.0
	)
};

vec4 mixLight(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) 
{
    lightDir0 = normalize(lightDir0);
    lightDir1 = normalize(lightDir1);
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, (light0 + light1) * LightPower + AmbientLight);
    vec3 finalCol = mix(color.rgb, SHADE, lightAccum);
    return vec4(vec3(color.r * lightAccum, color.g * lightAccum, color.b), color.a);
}

void main() 
{
	SideInfo info = sides.data[gl_InstanceID];
	
	vec4 transformedPos = vec4(Position, 1.0) * transformations[uint(info.side)];
	
	vec3 sideOffset = vec3(info.x, info.y, info.z);
    gl_Position = ProjMat * ModelViewMat * vec4(transformedPos.xyz * info.radius + sideOffset, 1.0);
    
    vec3 normal = normals[uint(info.side)];
	vertexColor = mixLight(Light0_Direction, Light1_Direction, normal, vec4(mix(DarknessColorModifier, vec3(1.0), info.brightness), 1.0));
}
