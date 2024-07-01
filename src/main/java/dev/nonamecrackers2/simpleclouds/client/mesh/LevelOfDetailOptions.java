package dev.nonamecrackers2.simpleclouds.client.mesh;

public enum LevelOfDetailOptions
{
	LOW(new CloudMeshGenerator.LevelOfDetailConfig(4, new CloudMeshGenerator.LevelOfDetail(2, 1), new CloudMeshGenerator.LevelOfDetail(4, 3), new CloudMeshGenerator.LevelOfDetail(8, 3))),
	MEDIUM(new CloudMeshGenerator.LevelOfDetailConfig(4, new CloudMeshGenerator.LevelOfDetail(2, 3), new CloudMeshGenerator.LevelOfDetail(4, 4), new CloudMeshGenerator.LevelOfDetail(8, 2))),
	HIGH(new CloudMeshGenerator.LevelOfDetailConfig(8, new CloudMeshGenerator.LevelOfDetail(2, 4), new CloudMeshGenerator.LevelOfDetail(4, 3), new CloudMeshGenerator.LevelOfDetail(8, 2)));
	
	private final CloudMeshGenerator.LevelOfDetailConfig config;
	
	private LevelOfDetailOptions(CloudMeshGenerator.LevelOfDetailConfig config)
	{
		this.config = config;
	}
	
	public CloudMeshGenerator.LevelOfDetailConfig getConfig()
	{
		return this.config;
	}
}
