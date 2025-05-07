package dev.nonamecrackers2.simpleclouds.client.mesh;

import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetail;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;

public enum LevelOfDetailOptions
{
	LOW(new LevelOfDetailConfig(4, new LevelOfDetail(2, 1), new LevelOfDetail(4, 3), new LevelOfDetail(8, 3))),
	MEDIUM(new LevelOfDetailConfig(4, new LevelOfDetail(2, 3), new LevelOfDetail(4, 4), new LevelOfDetail(8, 2))),
	HIGH(new LevelOfDetailConfig(8, new LevelOfDetail(2, 4), new LevelOfDetail(4, 3), new LevelOfDetail(8, 2)));
	
	private final LevelOfDetailConfig config;
	
	private LevelOfDetailOptions(LevelOfDetailConfig config)
	{
		this.config = config;
	}
	
	public LevelOfDetailConfig getConfig()
	{
		return this.config;
	}
}
