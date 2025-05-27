package dev.nonamecrackers2.simpleclouds.client.mesh.lod;

import dev.nonamecrackers2.simpleclouds.client.mesh.generator.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.world.phys.AABB;

public record PreparedChunk(int lodLevel, int lodScale, int x, int y, int z, int noOcclusionDirectionIndex, AABB bounds)
{
	public static PreparedChunk create(int lodLevel, int lodScale, int x, int y, int z, int noOcclusionDirectionIndex)
	{
		float chunkSize = (float)SimpleCloudsConstants.CHUNK_SIZE * lodScale;
		float maxY = (float)CloudMeshGenerator.VERTICAL_CHUNK_SPAN * (float)SimpleCloudsConstants.CHUNK_SIZE;
		float offsetX = (float)x * chunkSize;
		float offsetZ = (float)z * chunkSize;
		AABB bounds = new AABB(offsetX, -320.0F, offsetZ, offsetX + chunkSize, maxY, offsetZ + chunkSize);
		return new PreparedChunk(lodLevel, lodScale, x, y, z, noOcclusionDirectionIndex, bounds);
	}
}
