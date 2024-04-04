#version 150

#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

//in float vertexDistance;
in vec4 vertexColor;
in vec3 normal;

out vec4 fragColor;

void main() {
    vec4 color = ColorModulator * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color;//linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
