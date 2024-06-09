#version 150

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec3 normal;

out vec4 fragColor;

void main() {
    vec4 color = ColorModulator * vertexColor;
    color *= clamp(vertexDistance - 4000.0, 0.0, 1.0);
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color;
}
