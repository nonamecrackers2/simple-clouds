package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class VoronoiDiagramRegionType implements RegionType
{
	@Override
	public RegionType.Result getCloudTypeIndexAt(float posX, float posZ, float scale, int totalCloudTypes)
	{
		Vector2f pos = new Vector2f(posX, posZ).div(scale);
		Vector2f indexUv = pos.floor(new Vector2f());
		Vector2f fractUv = new Vector2f(pos).sub(indexUv);

		float minimumDist = 8.0F;  
		Vector2f closestCoord = null;
		Vector2f closestPoint = null;
		for (float y = -1.0F; y <= 1.0F; y++) 
		{
			for (float x = -1.0F; x <= 1.0F; x++) 
			{
				Vector2f neighbor = new Vector2f(x, y);
	            Vector2f point = new Vector2f(hash12(new Vector2f(indexUv).add(neighbor)) * 1.0F);
				Vector2f coord = new Vector2f(neighbor).add(point).sub(fractUv);
				float dist = coord.length();
				if (dist < minimumDist) 
				{
					minimumDist = dist;
	                closestCoord = coord;
	                closestPoint = point;
				}
			}
		}
	    minimumDist = 8.0F;
	    for (float y = -1.0F; y <= 1.0F; y++) 
		{
			for (float x = -1.0F; x <= 1.0F; x++) 
			{
				Vector2f neighbor = new Vector2f(x, y);
				Vector2f point = new Vector2f(hash12(new Vector2f(indexUv).add(neighbor)) * 1.0F);
				Vector2f coord = new Vector2f(neighbor).add(point).sub(fractUv);
				if (closestCoord.distance(coord) > 0.0F)
				{
					Vector2f firstTerm = new Vector2f(closestCoord).add(coord).mul(0.5F);
					Vector2f secondTerm = new Vector2f(coord).sub(closestCoord).normalize();
					float dot = firstTerm.dot(secondTerm);
					minimumDist = Math.min(minimumDist, dot);
				}
			}
		}
	    int index = (int)Math.floor(hash12(closestPoint) * (float)totalCloudTypes);
	    float fade = 1.0F - Math.min(minimumDist * 3.0F, 1.0F);
	    return new RegionType.Result(index, fade);
	}
	
	private static float hash12(Vector2f p)
	{
		Vector3f p3 = new Vector3f(p.x * 0.1031F, p.y * 0.1031F, p.x * 0.1031F);
		p3.sub(p3.floor(new Vector3f()));
		float dot = p3.dot(new Vector3f(p3.y + 33.33F, p3.z + 33.33F, p3.x + 33.33F));
		p3.add(dot, dot, dot);
		float finalResult = (p3.x + p3.y) * p3.z;
		return finalResult - Math.floor(finalResult);
	}
}
