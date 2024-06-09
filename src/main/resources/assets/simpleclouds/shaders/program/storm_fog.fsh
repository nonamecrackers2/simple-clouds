#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 WorldProjMat;
uniform mat4 ModelViewMat;
uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

#define CUBE_MODE true

vec2 raySphereIntersect(vec3 start, vec3 dir, float radius) 
{
    float a = dot(dir, dir);
    float b = 2.0 * dot(dir, start);
    float c = dot(start, start) - (radius * radius);
    float d = (b*b) - 4.0*a*c;
    if (d < 0.0) return vec2(1e12,-1e12);
    return vec2(
        (-b - sqrt(d))/(2.0*a),
        (-b + sqrt(d))/(2.0*a)
    );
}

// https://gist.github.com/DomNomNom/46bb1ce47f68d255fd5d
vec2 rayAABBIntersect(vec3 start, vec3 dir, float radius)
{
	vec3 tMin = (vec3(-radius) - start) / dir;
    vec3 tMax = (vec3(radius) - start) / dir;
    vec3 t1 = min(tMin, tMax);
    vec3 t2 = max(tMin, tMax);
    float tNear = max(max(t1.x, t1.y), t1.z);
    float tFar = min(min(t2.x, t2.y), t2.z);
    if (tNear > tFar)
    	return vec2(1e12, -1e12);
    return vec2(tNear, tFar);
}

vec3 getRayDirection(vec2 screenUV)
{
	vec2 uv = screenUV * 2.0 - 1.0;
	vec4 near = vec4(uv, 1.0, 1.0);
	vec4 far = vec4(uv, 0.0, 1.0);
	mat4 inverseProjMat = inverse(WorldProjMat);
	mat4 inverseModelViewMat = inverse(ModelViewMat);
	near = inverseProjMat * near;
	far = inverseProjMat * far;
	near.xyz /= near.w;
	far.xyz /= far.w;
	vec3 nearResult = (inverseModelViewMat * near).xyz;
	vec3 farResult = (inverseModelViewMat * far).xyz;
	return normalize(farResult - nearResult);
}

vec3 screenToWorldPos(vec2 coord, float depth)
{
	vec3 ndc = vec3(coord * 2.0 - 1.0, depth);
  	vec4 view = inverse(WorldProjMat) * vec4(ndc, 1.0);
  	view.xyz /= view.w;
  	vec3 result = (inverse(ModelViewMat) * view).xyz;
  	return result;
}

vec2 calculateIntersect(vec3 start, vec3 dir, float radius)
{
	if (CUBE_MODE)
		return rayAABBIntersect(start, dir, radius);
	else
		return raySphereIntersect(start, dir, radius);
}

float sceneDepth(vec3 pos, vec3 dir, float planetRadius) 
{
    float depth = 1e12;
    vec2 intersect = calculateIntersect(pos, dir, planetRadius); 
    if (0.0 < intersect.y)
    	return intersect.x;
	return depth;
}

void main() 
{
	vec4 col = texture(DiffuseSampler, texCoord);
	vec3 ray = getRayDirection(texCoord);
	vec3 pos = screenToWorldPos(texCoord, texture(DiffuseDepthSampler, texCoord).x * 2.0 - 1.0);
	float depth = length(pos);
	vec3 rayStart = -CameraPos + vec3(0.0, 100.0, 0.0);
	float sceneDepth = sceneDepth(rayStart, ray, 10.0);
	if (sceneDepth < depth && sceneDepth < 1e10)
		fragColor = vec4(1.0);
	else
		fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
}
