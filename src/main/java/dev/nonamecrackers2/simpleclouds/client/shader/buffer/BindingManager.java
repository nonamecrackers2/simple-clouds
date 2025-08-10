package dev.nonamecrackers2.simpleclouds.client.shader.buffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;

public class BindingManager
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/BindingManager");
	private static final IntList ALL_SHADER_STORAGE_BINDINGS = new IntArrayList();
	private static final IntList ALL_IMAGE_BINDINGS = new IntArrayList();
	private static int maxSSBOBindings = -1;
	private static int maxImageUnits = -1;
	
	public static void printDebug()
	{
		LOGGER.debug("Binded SSBOs: {}", ALL_SHADER_STORAGE_BINDINGS);
		LOGGER.debug("Binded image units: {}", ALL_IMAGE_BINDINGS);
	}
	
	public static void fillReport(CrashReport report)
	{
		CrashReportCategory category = report.addCategory("Simple Clouds Compute Shaders");
		category.setDetail("Binded SSBOS", ALL_SHADER_STORAGE_BINDINGS);
		category.setDetail("Binded Image Units", ALL_IMAGE_BINDINGS);
	}
	
	public static int getAvailableShaderStorageBinding()
	{
		RenderSystem.assertOnRenderThread();
		if (maxSSBOBindings == -1)
			maxSSBOBindings = GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS);
		// We go backwards to avoid conflicts with other mods using SSBO bindings
		for (int i = maxSSBOBindings - 1; i > 0; i--)
		{
			if (!ALL_SHADER_STORAGE_BINDINGS.contains(i))
				return i;
		}
		throw new NullPointerException("No available buffer binding. Total available buffer bindings: " + maxSSBOBindings + ", used: " + ALL_SHADER_STORAGE_BINDINGS.size());
	}
	
	public static void useShaderStorageBinding(int binding)
	{
		RenderSystem.assertOnRenderThread();
		if (ALL_SHADER_STORAGE_BINDINGS.contains(binding))
			throw new IllegalArgumentException("Binding " + binding + " is already in use");
		ALL_SHADER_STORAGE_BINDINGS.add(binding);
	}
	
	@SuppressWarnings("deprecation")
	public static void freeShaderStorageBinding(int binding)
	{
		RenderSystem.assertOnRenderThread();
		ALL_SHADER_STORAGE_BINDINGS.remove((Object)binding);
	}
	
	public static int getAvailableImageUnit()
	{
		RenderSystem.assertOnRenderThread();
		if (maxImageUnits == -1)
			maxImageUnits = GL11.glGetInteger(GL43.GL_MAX_IMAGE_UNITS);
		for (int i = maxImageUnits - 1; i > 0; i--)
		{
			if (!ALL_IMAGE_BINDINGS.contains(i))
				return i;
		}
		throw new NullPointerException("No available image binding. Total available image units: " + maxImageUnits);
	}
	
	public static int useImageUnit(int unit)
	{
		RenderSystem.assertOnRenderThread();
		ALL_IMAGE_BINDINGS.add(unit);
		return unit;
	}
	
	@SuppressWarnings("deprecation")
	public static void freeImageUnit(int unit)
	{
		RenderSystem.assertOnRenderThread();
		//We want to remove the value "unit" from the list, not the value at index "unit"
		ALL_IMAGE_BINDINGS.remove((Object)unit);
	}
	
	public static ShaderStorageBufferObject createSSBO(int usage)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		int binding = getAvailableShaderStorageBinding();
		int bufferId = GlStateManager._glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding, bufferId);
		ShaderStorageBufferObject buffer = new ShaderStorageBufferObject(bufferId, binding, usage);
		useShaderStorageBinding(binding);
		return buffer;
	}
	
	public static void freeSSBO(ShaderStorageBufferObject buffer)
	{
		RenderSystem.assertOnRenderThread();
		BindingManager.freeShaderStorageBinding(buffer.getBinding());
		buffer.close();
	}
}
