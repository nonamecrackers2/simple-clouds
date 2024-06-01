#version 430

#define LOCAL_SIZE vec3(${LOCAL_SIZE_X}, ${LOCAL_SIZE_Y}, ${LOCAL_SIZE_Z})
layout(local_size_x = ${LOCAL_SIZE_X}, local_size_y = ${LOCAL_SIZE_Y}, local_size_z = ${LOCAL_SIZE_Z}) in;

layout(r32ui, binding = 0) uniform uimage2D mainImage;

void main()
 {
    vec4 value = vec4(0.0, 0.0, 0.0, 1.0);
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
	
    value.x = float(texelCoord.x)/(gl_NumWorkGroups.x * LOCAL_SIZE.x);
    value.y = float(texelCoord.y)/(gl_NumWorkGroups.y * LOCAL_SIZE.y);
	
    imageStore(mainImage, texelCoord, ivec4(gl_GlobalInvocationID.x, 0, 0, 255));
}