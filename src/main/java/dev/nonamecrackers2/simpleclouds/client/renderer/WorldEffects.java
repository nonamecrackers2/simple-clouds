package dev.nonamecrackers2.simpleclouds.client.renderer;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class WorldEffects
{
	public static final float EFFECTS_STRENGTH_MULTIPLER = 1.2F;
	private static final int STORMINESS_DETECT_RADIUS = 1;
	private final Minecraft mc;
	private final SimpleCloudsRenderer renderer;
	private float storminessAtCamera;
	private float storminessSmoothed;
	private float storminessSmoothedO;
	
	protected WorldEffects(Minecraft mc, SimpleCloudsRenderer renderer)
	{
		this.mc = mc;
		this.renderer = renderer;
	}
	
	public void renderPost(float partialTick, double camX, double camY, double camZ, float scale)
	{
		//We can assume that the texture position we should fetch from is at the center of the shadow map texture, because
		//the shadow map should always be centered around the camera
//		int center = SimpleCloudsRenderer.SHADOW_MAP_SIZE / 2;
//		this.averageStorminess = this.renderer.getAverageStorminessInCoordArea(center - STORMINESS_DETECT_RADIUS, center - STORMINESS_DETECT_RADIUS, STORMINESS_DETECT_RADIUS * 2, STORMINESS_DETECT_RADIUS * 2);
//		this.storminessAtCamera = this.renderer.getStorminessAtCoord(center, center);
		
		CloudMeshGenerator generator = this.renderer.getMeshGenerator();
		CloudInfo info = generator.getCloudTypeAtOrigin();
		if (info != null && info.weatherType().causesDarkening() && (float)camY < info.stormStart() * scale + 32.0F)
		{
			float factor = Mth.clamp((1.0F - generator.getCloudFadeAtOrigin()) * 2.0F, 0.0F, 1.0F);
			this.storminessAtCamera = info.storminess() * factor;
		}
		else
		{
			this.storminessAtCamera = 0.0F;
		}
		
//		CloudManager manager = CloudManager.get(this.mc.level);
//		float scrollX = manager.getScrollX(partialTick);
//		float scrollZ = manager.getScrollZ(partialTick);
//		float chunkSizeUpscaled = 32.0F * SimpleCloudsRenderer.CLOUD_SCALE;
//		float camOffsetX = ((float)Mth.floor((float)this.mc.gameRenderer.getMainCamera().getPosition().x / chunkSizeUpscaled) * 32.0F);
//		float camOffsetZ = ((float)Mth.floor((float)this.mc.gameRenderer.getMainCamera().getPosition().z / chunkSizeUpscaled) * 32.0F);
//		Vector2f uv = new Vector2f(scrollX, scrollZ).add(-416.0F, -416.0F).add(camOffsetX, camOffsetZ).div(2000.0F);
		//System.out.println(this.random_vec2(new Vector2f(100.0F, 50.0F)));
		//System.out.println(this.getCloudTypeAt(uv, ClientSideCloudTypeManager.getInstance().getIndexed()));
	}
	
	//https://stackoverflow.com/a/17479300

//	public int hash(int x) 
//	{
//	    x += ( x << 10 );
//	    x ^= ( x >>  6 );
//	    x += ( x <<  3 );
//	    x ^= ( x >> 11 );
//	    x += ( x << 15 );
//	    return x;
//	}
//
//	// Compound versions of the hashing algorithm.
//	public int hash_vec2(Vector2i v ) { return hash( v.x ^ hash(v.y)                         ); }
//	public int hash_vec3(Vector3i v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z)             ); }
//	public int hash_vec4(Vector4i v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w) ); }
//
//	// Construct a float with half-open range [0:1] using low 23 bits.
//	// All zeroes yields 0.0, all ones yields the next smallest representable value below 1.0.
//	public float floatConstruct(int m ) 
//	{
//	    final int ieeeMantissa = 0x007FFFFF; // binary32 mantissa bitmask
//	    final int ieeeOne      = 0x3F800000; // 1.0 in IEEE binary32
//
//	    m &= ieeeMantissa;                     // Keep only mantissa bits (fractional part)
//	    m |= ieeeOne;                          // Add fractional part to 1.0
//
//	    float  f = Float.intBitsToFloat(m);       // Range [1:2]
//	    return f - 1.0F;                     // Range [0:1]
//	}
//
//	// Pseudo-random value in half-open range [0:1].
//	public float random( float x ) { return floatConstruct(hash((int)x)); }
//	public float random_vec2(Vector2f  v ) { return floatConstruct(hash_vec2(new Vector2i(Float.floatToIntBits(v.x), Float.floatToIntBits(v.y)))); }
//	public float random_vec3(Vector3f  v ) { return floatConstruct(hash_vec3(new Vector3i(Float.floatToIntBits(v.x), Float.floatToIntBits(v.y), Float.floatToIntBits(v.z)))); }
//	public float random_vec4(Vector4f  v ) { return floatConstruct(hash_vec4(new Vector4i(Float.floatToIntBits(v.x), Float.floatToIntBits(v.y), Float.floatToIntBits(v.z), Float.floatToIntBits(v.w)))); }
//	
//	public Pair<CloudType, Float> getCloudTypeAt(Vector2f pos, CloudType[] types) 
//	{
//	    Vector2f indexUv = pos.floor(new Vector2f());
//		Vector2f fractUv = pos.sub(indexUv, new Vector2f());
//
//		float minimumDist = 8.0F;  
//	    Vector2f closestCoord = null;
//	    Vector2f closestPoint = null;
//		for (float y = -1.0F; y <= 1.0F; y++) 
//		{
//			for (float x = -1.0F; x <= 1.0F; x++) 
//			{
//				Vector2f neighbor = new Vector2f(x, y);
//	            Vector2f point = new Vector2f(random_vec2(indexUv.add(neighbor, new Vector2f())) * 1.0F);
//				Vector2f coord = neighbor.add(point, new Vector2f()).sub(fractUv);
//				float dist = coord.length();
//				if (dist < minimumDist) 
//				{
//					minimumDist = dist;
//	                closestCoord = coord;
//	                closestPoint = point;
//				}
//			}
//		}
//	    minimumDist = 8.0F;
//	    for (float y = -1.0F; y <= 1.0F; y++) 
//		{
//			for (float x = -1.0F; x <= 1.0F; x++) 
//			{
//				Vector2f neighbor = new Vector2f(x, y);
//				Vector2f point = new Vector2f(random_vec2(indexUv.add(neighbor, new Vector2f())) * 1.0F);
//				Vector2f coord = neighbor.add(point, new Vector2f()).sub(fractUv);
//				if (closestCoord.distance(coord) > 0.0F)
//				{
//					float dot = closestCoord.add(coord, new Vector2f()).dot(coord.sub(closestCoord, new Vector2f()).normalize());
//					minimumDist = Math.min(minimumDist, dot);
//				}
//			}
//		}
//	    int index = Mth.floor(random_vec2(closestPoint) * types.length);
//	    float fade = 1.0F - Math.min(minimumDist * 3.0F, 1.0F);
//	    return Pair.of(types[index], fade);
//	}
	
	public float getStorminessAtCamera()
	{
		return this.storminessAtCamera;
	}
	
	public void tick()
	{
		this.storminessSmoothedO = this.storminessSmoothed;
		this.storminessSmoothed += (this.storminessAtCamera - this.storminessSmoothed) / 25.0F;
	}
	
	public float getStorminessSmoothed(float partialTick)
	{
		return Mth.lerp(partialTick, this.storminessSmoothedO, this.storminessSmoothed);
	}
	
	public float getSkyDarkenFactor(float partialTick)
	{
		return Mth.clamp(1.0F - this.getStorminessSmoothed(partialTick) * EFFECTS_STRENGTH_MULTIPLER, 0.1F, 1.0F);
	}
}
