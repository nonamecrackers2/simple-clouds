package dev.nonamecrackers2.simpleclouds.client.mesh;

public enum MeshStatus
{
	NOT_INITIALIZED("Not initialized", true, 4),
	NO_TASKS("No tasks", false, 1),
	NORMAL("Normal", false, 0),
	MESH_POOL_OVERFLOW("Mesh pool overflow", true, 2),
	CHUNK_OVERFLOW("Chunk overflow", true, 3);
	
	private String name;
	private boolean isErroneous;
	private int priority;
	
	private MeshStatus(String name, boolean isErroneous, int priority)
	{
		this.name = name;
		this.isErroneous = isErroneous;
		this.priority = priority;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public boolean isErroneous()
	{
		return this.isErroneous;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
}
