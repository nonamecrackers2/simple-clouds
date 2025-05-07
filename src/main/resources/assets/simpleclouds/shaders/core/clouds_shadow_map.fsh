#version 150

uniform vec4 ColorModulator;
uniform float HeightCutoff;

in vec4 vertexColor;
in float height;

out vec4 fragColor;

void main() 
{
    vec4 color = ColorModulator * vertexColor;
    if (height > HeightCutoff || color.a < 0.1)
        discard;
    fragColor = color;
}
