#version 150

uniform sampler2D DiffuseSampler;
uniform float Quality;
uniform float Directions;
uniform float Size;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

out vec4 fragColor;

void main() 
{
    float pi = 6.28318530718; // Pi*2
    
    float directions = Directions;
    float quality = Quality;
    float size = Size;
    vec2 radius = vec2(size) / InSize;
    
    vec4 color = texture(DiffuseSampler, texCoord);
    
    for (float d = 0.0; d < pi; d += pi / directions)
    {
		for (float i = 1.0 / quality; i <= 1.0; i += 1.0 / quality)
			color += texture(DiffuseSampler, texCoord + vec2(cos(d), sin(d)) * radius * i);		
    }
    
    color /= quality * directions - 15.0;
    
    fragColor = color;
}