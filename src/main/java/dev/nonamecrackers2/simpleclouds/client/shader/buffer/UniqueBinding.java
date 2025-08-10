package dev.nonamecrackers2.simpleclouds.client.shader.buffer;

public class UniqueBinding implements WithBinding
{
	private int binding = -1;
	
	public UniqueBinding(int binding)
	{
		this.binding = binding;
	}
	
	@Override
	public int getBinding()
	{
		return this.binding;
	}

	@Override
	public void close()
	{
		this.binding = -1;
	}
}
