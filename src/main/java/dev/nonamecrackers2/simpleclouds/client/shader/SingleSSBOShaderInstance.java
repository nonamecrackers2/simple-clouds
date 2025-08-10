package dev.nonamecrackers2.simpleclouds.client.shader;

import java.io.IOException;

import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.shader.buffer.BindingManager;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

public class SingleSSBOShaderInstance extends ShaderInstance
{
	private int binding = -1;
	
	public SingleSSBOShaderInstance(ResourceProvider provider, ResourceLocation shaderLocation, VertexFormat format, String ssboName) throws IOException
	{
		super(provider, shaderLocation, format);
		
		int index = GL43.glGetProgramResourceIndex(this.getId(), GL43.GL_SHADER_STORAGE_BLOCK, ssboName);
		if (index == -1)
			throw new NullPointerException("Unknown block index with name '" + ssboName + "'");
		this.binding = BindingManager.getAvailableShaderStorageBinding();
		GL43.glShaderStorageBlockBinding(this.getId(), index, this.binding);
		BindingManager.useShaderStorageBinding(this.binding);
	}
	
	public int getShaderStorageBinding()
	{
		return this.binding;
	}
	
	@Override
	public void close()
	{
		super.close();
		
		BindingManager.freeShaderStorageBinding(this.binding);
		this.binding = -1;
	}
}
