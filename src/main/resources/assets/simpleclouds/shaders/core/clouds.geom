#version 430
layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in VS_OUT {
    vec4 vertexColor;
} gs_in[];

out vec4 vertexColor;

void main()
{
    gl_Position = gl_in[0].gl_Position;
    vertexColor = gs_in[0].vertexColor;
    EmitVertex();
    gl_Position = gl_in[1].gl_Position;
    vertexColor = gs_in[1].vertexColor;
    EmitVertex();
    gl_Position = gl_in[2].gl_Position;
    vertexColor = gs_in[2].vertexColor;
    EmitVertex();
    EndPrimitive();
}
