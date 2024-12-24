package dev.nonamecrackers2.simpleclouds.client.mesh.lod;

public class LevelOfDetail
{
	private final int chunkScale;
	private final int spread;
	private int chunkCount;
	
	public LevelOfDetail(int chunkScale, int spread)
	{
		this.chunkScale = chunkScale;
		this.spread = spread;
	}
	
	public void setChunkCount(int count)
	{
		this.chunkCount = count;
	}
	
	public int chunkScale()
	{
		return this.chunkScale;
	}
	
	public int spread()
	{
		return this.spread;
	}
	
	public int chunkCount()
	{
		return this.chunkCount;
	}
}
