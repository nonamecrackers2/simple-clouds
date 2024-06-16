#version 150

#define SHADE vec3(0.6, 0.7, 0.8)

in vec3 Position;
in float Darkness;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform float LightPower;
uniform float AmbientLight;

//out float vertexDistance;
out vec4 vertexColor;
out vec4 normal;

vec4 mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) 
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
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    normal = ModelViewMat * vec4(Normal, 0.0);
	vertexColor = mix_light(Light0_Direction, Light1_Direction, Normal, vec4(vec3(Darkness), 1.0));
    //vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
}
