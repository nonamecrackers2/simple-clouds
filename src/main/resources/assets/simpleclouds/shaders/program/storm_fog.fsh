//Portions of this file are licensed under MIT

#version 430

// -- Lightning Flashes --

struct Lightning {
	vec3 Position;
	float Alpha;
};

layout(std430) readonly buffer LightningBolts {
	Lightning data[];
}
lightning;

uniform int TotalLightningBolts;

// ----------------------

uniform sampler2D ShadowMap;
uniform sampler2D ShadowMapColor;
uniform sampler2D DepthSampler;

uniform mat4 InverseWorldProjMat;
uniform mat4 InverseModelViewMat;
uniform mat4 ShadowProjMat;
uniform mat4 ShadowModelViewMat;

uniform vec3 CameraPos;

uniform float CutoffDistance;
uniform float FogStart;
uniform float FogEnd;

uniform vec3 ColorThreshold;
uniform float VerticalFade;
uniform vec3 ColorMultiplier;
uniform vec4 ColorModulator;

//The distance in which light can penetrate the storm fog, or
//a measure in which how far terrain must be in the storm fog
//to still be somewhat visible 
uniform float LightTransmittenceDistance;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

#define FOG 0 // 0 for no fog, 1 for fog
#define STEPS 200 //Total ray steps. More smaller steps means better accuracy and less artifacts
#define STEP_SIZE 40.0 //Size of each individual step
#define BG_COL vec4(0.0);

vec3 getRayDirection(vec2 screenUV)
{
	vec2 uv = screenUV * 2.0 - 1.0;
	vec4 near = vec4(uv, 0.0, 1.0);
	vec4 far = vec4(uv, 1.0, 1.0);
	near = InverseWorldProjMat * near;
	far = InverseWorldProjMat * far;
	near.xyz /= near.w;
	far.xyz /= far.w;
	vec3 nearResult = (InverseModelViewMat * near).xyz;
	vec3 farResult = (InverseModelViewMat * far).xyz;
	return normalize(farResult - nearResult);
}

vec4 shadowMapColorAt(vec3 pos)
{
	vec4 shadowMapPos = ShadowProjMat * ShadowModelViewMat * vec4(pos, 1.0);
	vec3 ndc = shadowMapPos.xyz / shadowMapPos.w;
	vec3 coord = ndc * 0.5 + 0.5;
	float shadowMapDepth = texture(ShadowMap, coord.xy).x;
	if (shadowMapDepth < 1.0 && shadowMapDepth < coord.z)
		return vec4(texture(ShadowMapColor, coord.xy).rgb, 1.0);
	return vec4(0.0);
}

//---------------------------------------------
// The MIT License
// Copyright © 2016 Inigo Quilez
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// https://www.shadertoy.com/view/4lcSRn
vec4 cylinderVerticalIntersect(in vec3 ro, in vec3 rd, float he, float ra)
{
    float k2 = 1.0        - rd.y*rd.y;
    float k1 = dot(ro,rd) - ro.y*rd.y;
    float k0 = dot(ro,ro) - ro.y*ro.y - ra*ra;
    
    float h = k1*k1 - k2*k0;
    if( h<0.0 ) return vec4(-1.0);
    h = sqrt(h);
    float t = (-k1-h)/k2;

    // body
    float y = ro.y + t*rd.y;
    if( y>-he && y<he ) return vec4( t, (ro + t*rd - vec3(0.0,y,0.0))/ra );
    
    // caps
    t = ( ((y<0.0)?-he:he) - ro.y)/rd.y;
    if( abs(k1+k2*t)<h ) return vec4( t, vec3(0.0,sign(y),0.0) );

    return vec4(-1.0);
}
//---------------------------------------------

float getNearestLightningBoltColorModifier(vec3 position)
{
	for (int i = 0; i < TotalLightningBolts; i++)
	{
		Lightning bolt = lightning.data[i];
		float dist = distance(bolt.Position.xz, position.xz);
		if (dist < 2000.0)
		{
			float distMul = clamp(2.0 - dist * 0.001, 0.0, 1.0);
			return 1.0 + bolt.Alpha * distMul;
		}
	}
	return 1.0;
}

vec3 screenToWorldPos(vec2 coord, float depth)
{
	vec3 ndc = vec3(coord * 2.0 - 1.0, depth);
  	vec4 view = InverseWorldProjMat * vec4(ndc, 1.0);
  	view.xyz /= view.w;
  	vec3 result = (InverseModelViewMat * view).xyz;
  	return result;
}

void main() 
{
	vec3 rayDir = getRayDirection(texCoord);
	vec4 rayStartDist = cylinderVerticalIntersect(vec3(0.0), rayDir, 500.0, CutoffDistance);
	vec3 point = CameraPos + rayDir;// * -rayStartDist.x;
	float sceneDepth = length(screenToWorldPos(texCoord, texture(DepthSampler, texCoord).r * 2.0 - 1.0));
	float rayDepth = distance(point, CameraPos);
	
	float density = 0.0;
	vec3 colorAccum = vec3(0.0);
	float fogSteps = 0.0;
	
    for (int i = 0; i < STEPS; i++)
    {
    	point = CameraPos + rayDir * 0.2 * pow(float(i), 2.0);
    	rayDepth = distance(point, CameraPos);
    	
    	//If at any point the ray intersects with any vertex in the scene, we stop
    	if (sceneDepth < rayDepth)
    		break;
    		
    	vec4 col = shadowMapColorAt(point); //Here we get the color of the shadow map at the position of the ray
    	
    	//If there is a shadow at this point (alpha > 0) and the color is within our designated threshold, we stop
    	//and set the final color
    	if (col.a > 0.0 && col.r <= ColorThreshold.r && col.g <= ColorThreshold.g && col.b <= ColorThreshold.b)
    	{
    		float densityAdd = rayDepth * 0.0001;
    		
    		float fadeFactor = 1.0;
    		
    		// Distance fade
			if (rayDepth > FogStart)
				fadeFactor = 1.0 - min(pow((rayDepth - FogStart) / (FogEnd - FogStart), 0.05), 1.0);
				
			// Vertical fade to prevent the storm fog from stretching down too far
    		fadeFactor *= clamp(1.0 + point.y / VerticalFade, 0.0, 1.0);
    		
    		density += densityAdd * fadeFactor;
    		
    		fogSteps += 1.0;
			colorAccum += vec3(col.rgb * ColorMultiplier * ColorModulator.rgb);
    	}
    	
    	if (density >= 1.0)
    		break;
    }
    
    if (fogSteps <= 0.0)
    {
    	fragColor = BG_COL;
    	return;
    }
    
    vec3 avgCol = colorAccum / fogSteps;
    vec4 finalCol = vec4(avgCol, min(density, 1.0));
    
    // This is technically not correct but looks ok
    float lightningMul = getNearestLightningBoltColorModifier(point);
	finalCol.rgb *= lightningMul;

	fragColor = finalCol;
}
