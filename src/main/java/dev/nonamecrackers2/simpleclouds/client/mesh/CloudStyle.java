package dev.nonamecrackers2.simpleclouds.client.mesh;

public enum CloudStyle
{
	DEFAULT(0),
	SHADED(1);
	
	private final int index;
	
	private CloudStyle(int index)
	{
		this.index = index;
	}
	
	public int getIndex()
	{
		return this.index;
	}
}
