package dev.nonamecrackers2.simpleclouds.common.cloud;

import net.minecraft.util.StringRepresentable;

public enum CloudMode implements StringRepresentable
{
	DEFAULT("default"),
	SINGLE("single"),
	AMBIENT("ambient");
	
	private final String id;
	
	private CloudMode(String id)
	{
		this.id = id;
	}

	@Override
	public String getSerializedName()
	{
		return this.id;
	}
}
