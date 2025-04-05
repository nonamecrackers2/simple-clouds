package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.List;
import java.util.function.BiPredicate;

import org.joml.Vector2f;
import org.joml.Vector2i;

import com.google.common.collect.Lists;

import net.minecraft.util.RandomSource;

public record SpawnRegion(int x, int z, int radius)
{
	public boolean includesPoint(int x, int z)
	{
		return x >= this.getMinX() && x <= this.getMaxX() && z >= this.getMinZ() && z <= this.getMaxZ();
	}
	
	public boolean intersectsCircle(float x, float z, float radius)
	{
		float dx = Math.abs(x - this.x);
		float dz = Math.abs(z - this.z);
	
		if (dx > (float)this.radius + radius || dz > (float)this.radius + radius)
			return false;
		
		if (dx <= (float)this.radius || dz < (float)this.radius)
			return true;
		
		float cornerDist = Vector2f.distanceSquared(dx, dz, (float)this.radius, (float)this.radius);
		return cornerDist <= radius * radius;
	}
	
	public int getMinX()
	{
		return this.x - this.radius;
	}
	
	public int getMaxX()
	{
		return this.x + this.radius;
	}
	
	public int getMinZ()
	{
		return this.z - this.radius;
	}
	
	public int getMaxZ()
	{
		return this.z + this.radius;
	}
	
	public static void randomPointForEachRegion(Iterable<SpawnRegion> regions, RandomSource random, int maxAttemptsPerRegion, BiPredicate<SpawnRegion, Vector2i> consumer)
	{
		List<Vector2i> prevPositions = Lists.newArrayList();
		for (SpawnRegion region : regions)
		{
			if (prevPositions.stream().anyMatch(pos -> region.includesPoint(pos.x, pos.y)))
				continue;
			for (int i = 0; i < maxAttemptsPerRegion; i++)
			{
				Vector2i pos = getRandomPointInRegion(region, random);
				if (consumer.test(region, pos))
				{
					prevPositions.add(pos);
					break;
				}
			}
		}
	}
	
	public static Vector2i getRandomPointInRegion(SpawnRegion region, RandomSource random)
	{
		int x = random.nextInt(region.radius() * 2) - region.radius() + region.x();
		int z = random.nextInt(region.radius() * 2) - region.radius() + region.z();
		return new Vector2i(x, z);
	}
	
	public static boolean doesCircleIntersect(Iterable<SpawnRegion> regions, float x, float z, float radius)
	{
		for (SpawnRegion region : regions)
		{
			if (region.intersectsCircle(x, z, radius))
				return true;
		}
		return false;
	}
}
