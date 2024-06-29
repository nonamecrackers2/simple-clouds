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

void createFace(vec3 offset, vec3 corner1, vec3 corner2, vec3 corner3, vec3 corner4, vec3 normal, float brightness)
{
	uint currentFace = atomicAdd(counter, 1u);
	uint lastIndex = currentFace * 6;
	uint lastVertex = currentFace * 4;
	Side side;
	side.a = Vertex(offset.x + corner1.x, offset.y + corner1.y, offset.z + corner1.z, brightness, normal.x, normal.y, normal.z);
	side.b = Vertex(offset.x + corner2.x, offset.y + corner2.y, offset.z + corner2.z, brightness, normal.x, normal.y, normal.z);
	side.c = Vertex(offset.x + corner3.x, offset.y + corner3.y, offset.z + corner3.z, brightness, normal.x, normal.y, normal.z);
	side.d = Vertex(offset.x + corner4.x, offset.y + corner4.y, offset.z + corner4.z, brightness, normal.x, normal.y, normal.z);
	sides.data[currentFace] = side;
	for (uint i = 0; i < sideIndices.length; i++)
		indices.data[lastIndex + i] = lastVertex + sideIndices[i];
}

void createCube(float x, float y, float z, float cubeRadius, float brightness, float fade, LayerGroup group)
{
	vec3 norm = normalize(vec3(x, y, z) - Origin);
	vec3 offset = vec3(x + cubeRadius, y + cubeRadius, z + cubeRadius);
	//-Y
	if (!isPosValid(x, y - Scale, z, group, fade) || shouldNotOcclude(2))
		createFace(offset, vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(0.0, -1.0, 0.0), brightness);
	//+Y
	if (!isPosValid(x, y + Scale, z, group, fade) || shouldNotOcclude(3))
		createFace(offset, vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(0.0, 1.0, 0.0), brightness);
	//-X
	if (dot(norm, vec3(-1.0, 0.0, 0.0)) <= 0.0 && (!isPosValid(x - Scale, y, z, -1, 0) || shouldNotOcclude(0)))
		createFace(offset, vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(-1.0, 0.0, 0.0), brightness);
	//+X
	if (dot(norm, vec3(1.0, 0.0, 0.0)) <= 0.0 && (!isPosValid(x + Scale, y, z, 1, 0) || shouldNotOcclude(1)))
		createFace(offset, vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(1.0, 0.0, 0.0), brightness);
	//-Z
	if (dot(norm, vec3(0.0, 0.0, -1.0)) <= 0.0 && (!isPosValid(x, y, z - Scale, 0, -1) || shouldNotOcclude(4)))
		createFace(offset, vec3(-cubeRadius, -cubeRadius, -cubeRadius), vec3(-cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, cubeRadius, -cubeRadius), vec3(cubeRadius, -cubeRadius, -cubeRadius), vec3(0.0, 0.0, -1.0), brightness);
	//+Z
	if (dot(norm, vec3(0.0, 0.0, 1.0)) <= 0.0 && (!isPosValid(x, y, z + Scale, 0, 1) || shouldNotOcclude(5)))
		createFace(offset, vec3(cubeRadius, -cubeRadius, cubeRadius), vec3(cubeRadius, cubeRadius, cubeRadius), vec3(-cubeRadius, cubeRadius, cubeRadius), vec3(-cubeRadius, -cubeRadius, cubeRadius), vec3(0.0, 0.0, 1.0), brightness);
}

void main() 
{
    vec3 id = gl_GlobalInvocationID;
    float x = id.x * Scale + RenderOffset.x;
    float y = id.y * Scale + RenderOffset.y;
    float z = id.z * Scale + RenderOffset.z;
    
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xz + RegionSampleOffset);
    vec4 info = imageLoad(regions, ivec3(texelCoord, LodLevel));
    uint regionId = uint(info.r);
    LayerGroup group = layerGroupings.data[regionId];
    float fade = -5.0 * pow(info.g, 10.0);
    if (isPosValid(x, y, z, group, fade))
    {
    	float brightness = 1.0 - group.Storminess * (1.0 - clamp((y - group.StormStart) / group.StormFadeDistance, 0.0, 1.0));
    	createCube(x, y, z, Scale / 2.0, brightness, fade, group);
    }
}