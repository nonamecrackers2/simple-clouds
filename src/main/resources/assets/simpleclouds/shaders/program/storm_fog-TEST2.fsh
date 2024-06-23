#version 330

uniform sampler2D ShadowMap;
uniform sampler2D ShadowMapColor;
uniform mat4 WorldProjMat;
uniform mat4 ModelViewMat;
uniform mat4 ShadowProjMat;
uniform mat4 ShadowModelViewMat;
uniform vec3 CameraPos;
uniform float FogStart;
uniform float FogEnd;

in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

vec3 getRayDirection(vec2 screenUV)
{
	vec2 uv = screenUV * 2.0 - 1.0;
	vec4 near = vec4(uv, 0.0, 1.0);
	vec4 far = vec4(uv, 1.0, 1.0);
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

vec4 shadowMapColorAt(vec3 pos)
{
	vec4 shadowMapPos = ShadowProjMat * ShadowModelViewMat * vec4(pos, 1.0);
	vec3 ndc = shadowMapPos.xyz / shadowMapPos.w;
	vec3 coord = ndc * 0.5 + 0.5;
	float shadowMapDepth = texture(ShadowMap, coord.xy).x;
	if (shadowMapDepth < coord.z)
		return vec4(texture(ShadowMapColor, coord.xy).rgb, 1.0);
	else
		return vec4(0.0);
}

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

void main() 
{
	//vec3 pos = screenToWorldPos(texCoord, texture(DiffuseDepthSampler, texCoord).x * 2.0 - 1.0);
	//float depth = length(pos);
	vec3 ray = getRayDirection(texCoord);
	vec4 intersect = cylinderVerticalIntersect(vec3(0.0), ray, 500.0, 500.0);
	vec3 rayStart = CameraPos + ray * -intersect.x;
	//if (distance(rayStart, CameraPos) > depth)
	//{
	//	fragColor = vec4(0.0);
	//	return;
	//}
	vec3 point = rayStart;
	//float prevDepth = distance(rayStart, CameraPos);
	vec4 finalCol = vec4(0.0);
	float depth = 0.0;
    for (int i = 0; i < 250; i++)
    {
    	point += ray * 40.0;//(10.0 + pow(1.02, float(i)));
    	depth = distance(point, CameraPos);
    	//float currentDepth = distance(point, CameraPos);
    	//prevDepth = currentDepth;
    	//if (currentDepth > depth)
    	//	break;
    	vec4 col = shadowMapColorAt(point);	
    	if (col.a > 0.0 && length(col.rgb) <= 0.7)
    	{
			finalCol = vec4(col.rgb * 0.8, clamp(1.0 + point.y / 400.0, 0.0, 1.0));
    		break;
    	}
    }
    
    if (finalCol.a > 0.0)
    {
	    float fogFactor = 1.0 - min(max(depth - FogStart, 0.0) / (FogEnd - FogStart), 1.0);
	   // finalCol = vec4(finalCol.rgb, fogFactor);
    }
    
	fragColor = finalCol;
}
