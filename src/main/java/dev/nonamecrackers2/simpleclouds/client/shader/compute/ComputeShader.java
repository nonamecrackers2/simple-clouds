package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.client.ForgeHooksClient;

public class ComputeShader implements AutoCloseable
{
	protected static final Logger LOGGER = LogManager.getLogger("simpleclouds/ComputeShader");
	private static final Pattern LOCAL_GROUP_REPLACER = Pattern.compile("\\$\\{.*?\\}");
	private static final Map<String, ComputeShader.CompiledShader> COMPILED_PROGRAMS = Maps.newHashMap();
	private static final Int2ObjectMap<ShaderStorageBufferObject> ALL_SHADER_STORAGE_BUFFERS = new Int2ObjectOpenHashMap<>();
	private static final IntList ALL_IMAGE_BINDINGS = new IntArrayList();
	private static int maxGroupX = -1;
	private static int maxGroupY = -1;
	private static int maxGroupZ = -1;
	private static int maxLocalGroupX = -1;
	private static int maxLocalGroupY = -1;
	private static int maxLocalGroupZ = -1;
	private static int maxLocalInvocations = -1;
	private int id;
	private final ComputeShader.CompiledShader compiledShader;
	private final String name;
	private final Map<String, ShaderStorageBufferObject> shaderStorageBuffers = Maps.newHashMap();
//	private final Map<String, Integer> imageBuffers = Maps.newHashMap();
	private final List<String> missingUniformErrors = Lists.newArrayList();
	
	private ComputeShader(int id, ComputeShader.CompiledShader compiledShader, String name)
	{
		this.id = id;
		this.compiledShader = compiledShader;
		this.compiledShader.attachToShader(this);
		this.name = name;
	}
	
	public static int getAvailableShaderStorageBinding()
	{
		int max = GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS);
		for (int i = 0; i < max; i++)
		{
			if (!ALL_SHADER_STORAGE_BUFFERS.containsKey(i))
				return i;
		}
		throw new NullPointerException("No available buffer binding. Total available buffer bindings: " + max);
	}
	
	public static int getAvailableImageUnit()
	{
		int max = GL11.glGetInteger(GL43.GL_MAX_IMAGE_UNITS);
		for (int i = 0; i < max; i++)
		{
			if (!ALL_IMAGE_BINDINGS.contains(i))
				return i;
		}
		throw new NullPointerException("No available image binding. Total available image units: " + max);
	}
	
	public static int getAndUseImageUnit()
	{
		int unit = getAvailableImageUnit();
		ALL_IMAGE_BINDINGS.add(unit);
		return unit;
	}
	
	public static void freeImageUnit(int unit)
	{
		ALL_IMAGE_BINDINGS.removeInt(unit);
	}
	
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		LOGGER.debug("Closing compute shader id={}", this.id);
		this.shaderStorageBuffers.values().forEach(buffer -> 
		{
			buffer.close();
			ALL_SHADER_STORAGE_BUFFERS.remove(buffer.getBinding());
		});
		this.shaderStorageBuffers.clear();
//		this.imageBuffers.values().forEach(binding -> {
//			ALL_IMAGE_BINDINGS.remove(binding);
//		});
//		this.imageBuffers.clear();
		if (this.id != -1)
		{
			GlStateManager.glDeleteProgram(this.id);
			this.id = -1;
		}
		this.compiledShader.close();
		this.missingUniformErrors.clear();
	}
	
	public void forUniform(String name, IntConsumer consumer)
	{
		ProgramManager.glUseProgram(this.id);
		int loc = GlStateManager._glGetUniformLocation(this.id, name);
		if (loc == -1 && !this.missingUniformErrors.contains(name))
		{
			LOGGER.warn("Could not find uniform with name '{}'", name);
			this.missingUniformErrors.add(name);
		}
		else
		{
			consumer.accept(loc);
		}
		ProgramManager.glUseProgram(0);
	}
	
	/**
	 * Automatically creates a shader storage buffer using the next available binding in the context. 
	 * In other words, makes a new SSBO unique to this shader.
	 * 
	 * @param name The name of the block in the shader.
	 * @param usage One of:<br><table><tr><td>{@link GL15C#GL_STREAM_DRAW STREAM_DRAW}</td><td>{@link GL15C#GL_STREAM_READ STREAM_READ}</td><td>{@link GL15C#GL_STREAM_COPY STREAM_COPY}</td><td>{@link GL15C#GL_STATIC_DRAW STATIC_DRAW}</td><td>{@link GL15C#GL_STATIC_READ STATIC_READ}</td><td>{@link GL15C#GL_STATIC_COPY STATIC_COPY}</td><td>{@link GL15C#GL_DYNAMIC_DRAW DYNAMIC_DRAW}</td></tr><tr><td>{@link GL15C#GL_DYNAMIC_READ DYNAMIC_READ}</td><td>{@link GL15C#GL_DYNAMIC_COPY DYNAMIC_COPY}</td></tr></table>
	 * @return {@link ShaderStorageBufferObject}
	 */
	public ShaderStorageBufferObject bindShaderStorageBuffer(String name, int usage)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		this.assertValid();
		if (this.shaderStorageBuffers.containsKey(name))
			throw new IllegalArgumentException("Buffer with name '" + name + "' is already defined");
		int index = GL43.glGetProgramResourceIndex(this.id, GL43.GL_SHADER_STORAGE_BLOCK, name);
		if (index == -1)
			throw new NullPointerException("Unknown block index with name '" + name + "'");
		int binding = getAvailableShaderStorageBinding();
		GL43.glShaderStorageBlockBinding(this.id, index, binding);
		int bufferId = GlStateManager._glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding, bufferId);
		ShaderStorageBufferObject buffer = new ShaderStorageBufferObject(bufferId, binding, usage);
		this.shaderStorageBuffers.put(name, buffer);
		ALL_SHADER_STORAGE_BUFFERS.put(binding, buffer);
		return buffer;
	}
	
	public void setImageUnit(String name, int unit)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		this.assertValid();
		int loc = GL20.glGetUniformLocation(this.id, name);
		if (loc == -1)
			throw new NullPointerException("Unknown image with name '" + name + "'");
		GL41.glProgramUniform1i(this.id, loc, unit);
	}
	
//	public void removeImageUnit(String name, int textureId)
//	{
//		if (this.imageBuffers.containsKey(name))
//		{
//			int binding = this.imageBuffers.remove(name);
//			ALL_IMAGE_BINDINGS.remove(binding);
//		}
//	}
//	
//	public void bindAndAssignImageUnit(String name, int usage, int textureId, int level, boolean layered, int layer, int format)
//	{
//		RenderSystem.assertOnRenderThreadOrInit();
//		this.assertValid();
//		if (this.imageBuffers.containsKey(name))
//			throw new IllegalArgumentException("Image with name '" + name + "' is already defined");
//		int loc = GL20.glGetUniformLocation(this.id, name);
//		if (loc == -1)
//			throw new NullPointerException("Unknown image with name '" + name + "'");
//		int binding = -1;
//		binding = getAvailableImageBinding();
//		GL42.glBindImageTexture(binding, textureId, level, layered, layer, usage, format);
//		ALL_IMAGE_BINDINGS.put(binding, textureId);
//		GL41.glProgramUniform1i(this.id, loc, binding);
//		this.imageBuffers.put(name, binding);
//	}
//	
//	public void assignImageUnit(String name, int textureId)
//	{
//		RenderSystem.assertOnRenderThreadOrInit();
//		this.assertValid();
//		if (this.imageBuffers.containsKey(name))
//			throw new IllegalArgumentException("Image with name '" + name + "' is already defined");
//		int loc = GL20.glGetUniformLocation(this.id, name);
//		if (loc == -1)
//			throw new NullPointerException("Unknown image with name '" + name + "'");
//		int binding = -1;
//		for (var entry : ALL_IMAGE_BINDINGS.int2IntEntrySet())
//		{
//			if (entry.getIntValue() == textureId)
//				binding = entry.getIntKey();
//		}
//		if (binding == -1)
//			throw new IllegalStateException("Image is not binded!");
//		GL41.glProgramUniform1i(this.id, loc, binding);
//		this.imageBuffers.put(name, binding);
//	}
//	
	public ShaderStorageBufferObject getShaderStorageBuffer(String name)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		return Objects.requireNonNull(this.shaderStorageBuffers.get(name), "Unknown buffer with name '" + name + "'");
	}
	
	public void dispatch(int groupX, int groupY, int groupZ, boolean wait)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		if (maxGroupX == -1 || maxGroupY == -1 || maxGroupZ == -1)
		{
			maxGroupX = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
			maxGroupY = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1);
			maxGroupZ = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2);
			LOGGER.debug("Max work group sizes: x={}, y={}, z={}", maxGroupX, maxGroupY, maxGroupZ);
		}
		if (groupX > maxGroupX || groupY > maxGroupY || groupZ > maxGroupZ)
			throw new IllegalArgumentException("Work group count too large! Wanted: x=" + groupX + ", y=" + groupY + ", z=" + groupZ + "; Max allowed: x=" + maxGroupX + ", y=" + maxGroupY + ", z=" + maxGroupZ);
		else if (groupX <= 0 || groupY <= 0 || groupZ <= 0)
			throw new IllegalArgumentException("Work group count must be greater than zero!");
		ProgramManager.glUseProgram(this.id);
		GL43.glDispatchCompute(groupX, groupY, groupZ);
		if (wait)
			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_ATOMIC_COUNTER_BARRIER_BIT | GL42.GL_UNIFORM_BARRIER_BIT | GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		ProgramManager.glUseProgram(0);
	}
	
	public void dispatchAndWait(int groupX, int groupY, int groupZ)
	{
		this.dispatch(groupX, groupY, groupZ, true);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public int getId()
	{
		return this.id;
	}
	
	@Override
	public String toString()
	{
		return "ComputeShader[id=" + this.id + ", name=" + this.name + "]";
	}
	
	private void assertValid()
	{
		if (!this.isValid())
			throw new IllegalStateException("Compute shader is no longer valid!");
	}
	
	public boolean isValid()
	{
		return this.id != -1 && this.compiledShader.getId() != -1;
	}
	
	public static ComputeShader loadShader(ResourceLocation loc, ResourceProvider provider, int localX, int localY, int localZ) throws IOException
	{
		return loadShader(loc, provider, localX, localY, localZ, ImmutableMap.of());
	}
	
	public static ComputeShader loadShader(ResourceLocation loc, ResourceProvider provider, int localX, int localY, int localZ, ImmutableMap<String, String> parameters) throws IOException
	{
		if (maxLocalGroupX == -1 || maxLocalGroupY == -1 || maxLocalGroupZ == -1)
		{
			maxLocalGroupX = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0);
			maxLocalGroupY = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1);
			maxLocalGroupZ = GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2);
			LOGGER.debug("Max local group sizes: x={}, y={}, z={}", maxLocalGroupX, maxLocalGroupY, maxLocalGroupZ);
		}
		if (maxLocalInvocations == -1)
		{
			maxLocalInvocations = GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
			LOGGER.debug("Max local group invocations: {}", maxLocalInvocations);
		}
		if (localX > maxLocalGroupX || localY > maxLocalGroupY || localZ > maxLocalGroupZ)
			throw new IOException("Local group count too large! Wanted: x=" + localX + ", y=" + localY + ", z=" + localZ + "; Max allowed: x=" + maxLocalGroupX + ", y=" + maxLocalGroupY + ", z=" + maxLocalGroupZ);
		else if (localX <= 0 || localY <= 0 || localZ <= 0)
			throw new IOException("Local group size must be greater than zero!");
		if (localX * localY * localZ > maxLocalInvocations)
			throw new IOException("The amount of local invocations (X * Y * Z) is greater than the maximum allowed! Wanted: " + localX * localY * localZ + "; Allowed: " + maxLocalInvocations);

		String rawLoc = loc.toString();
		String compiledShaderId = rawLoc + ",params:" + parameters + ",local_size_x=" + localX + ",local_size_y=" + localY + ",local_size_z=" + localZ;
		ComputeShader.CompiledShader compiledShader;
		if (COMPILED_PROGRAMS.containsKey(compiledShaderId))
		{
			compiledShader = COMPILED_PROGRAMS.get(compiledShaderId);
		}
		else
		{
			String path = "shaders/compute/" + loc.getPath() + ".csh";
			ResourceLocation finalLoc = new ResourceLocation(loc.getNamespace(), path);
			Resource resource = provider.getResourceOrThrow(finalLoc);
				
			try (InputStream inputStream = resource.open())
			{
				final String fullPath = FileUtil.getFullResourcePath(path);
				compiledShader = compileShader(loc.toString(), compiledShaderId, inputStream, resource.sourcePackId(), new GlslPreprocessor()
				{
					private final Set<String> importedPaths = Sets.newHashSet();
		
					@Override
					public List<String> process(String file)
					{
						file = LOCAL_GROUP_REPLACER.matcher(file).replaceAll(result -> {
							String group = result.group();
							for (var entry : parameters.entrySet())
							{
								if (entry.getKey().equals(group))
									return entry.getValue();
							}
							switch (group)
							{
							case "${LOCAL_SIZE_X}":
								return String.valueOf(localX);
							case "${LOCAL_SIZE_Y}":
								return String.valueOf(localY);
							case "${LOCAL_SIZE_Z}":
								return String.valueOf(localZ);
							default:
							{
								LOGGER.error("Unknown variable '{}'" + group);
								return group;
							}
							}
						});
						return super.process(file);
					}
					
					public String applyImport(boolean isRelative, String importPath)
					{
						ResourceLocation glslImport = ForgeHooksClient.getShaderImportLocation(fullPath, isRelative, importPath);
						if (!this.importedPaths.add(glslImport.toString()))
						{
							return null;
						}
						else
						{
							try (Reader reader = provider.openAsReader(glslImport))
							{
								return IOUtils.toString(reader);
							}
							catch (IOException ioexception)
							{
								LOGGER.error("Could not open GLSL import {}: {}", glslImport, ioexception.getMessage());
								return "#error " + ioexception.getMessage();
							}
						}
					}
				});
				COMPILED_PROGRAMS.put(compiledShaderId, compiledShader);
			}
		}
		
		int programId = ProgramManager.createProgram();
		ComputeShader shader = new ComputeShader(programId, compiledShader, loc.toString());
		GlStateManager.glLinkProgram(programId);
		int i = GlStateManager.glGetProgrami(programId, GL20.GL_LINK_STATUS);
		if (i == 0)
		{
			LOGGER.warn("Error encountered when linking program containing computer shader {}. Log output:", loc);
			LOGGER.warn(GlStateManager.glGetProgramInfoLog(programId, GL11.GL_HINT_BIT));
		}
		return shader;
	}
	
	private static ComputeShader.CompiledShader compileShader(String loc, String id, InputStream inputStream, String packId, GlslPreprocessor preprocessor) throws IOException
	{
		RenderSystem.assertOnRenderThread();
		String file = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		if (file == null)
			throw new IOException("Could not load compute shader '" + loc + "'");
		int shaderId = GlStateManager.glCreateShader(GL43.GL_COMPUTE_SHADER);
		GlStateManager.glShaderSource(shaderId, preprocessor.process(file));
		GlStateManager.glCompileShader(shaderId);
		if (GlStateManager.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0)
		{
			String error = StringUtils.trim(GL20.glGetShaderInfoLog(shaderId, GL11.GL_HINT_BIT));
			throw new IOException("Couldn't compile compute shader (" + packId + ", " + loc + ") : " + error);
		}
		return new ComputeShader.CompiledShader(shaderId, id);
	}
	
	public static void destroyCompiledShaders()
	{
		var iterator = COMPILED_PROGRAMS.values().iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().destroy())
				iterator.remove();
		}
	}
	
	public static class CompiledShader
	{
		private final String shaderId;
		private int id;
		private int references;
		
		protected CompiledShader(int id, String shaderId)
		{
			this.id = id;
			this.shaderId = shaderId;
		}
		
		public int getId()
		{
			return this.id;
		}
		
		public void attachToShader(ComputeShader shader)
		{
			if (this.id != -1)
			{
				this.references++;
				RenderSystem.assertOnRenderThread();
				GlStateManager.glAttachShader(shader.getId(), this.id);
				LOGGER.debug("Attached compiled shader id={} to compute shader id={}, total references={}", this.id, shader.getId(), this.references);
			}
		}
		
		public void close()
		{
			if (this.id == -1)
				return;
			this.references--;
			if (this.references <= 0)
			{
				if (this.destroy())
					COMPILED_PROGRAMS.remove(this.shaderId);
			}
		}
		
		private boolean destroy()
		{
			if (this.id != -1)
			{
				RenderSystem.assertOnRenderThread();
				GlStateManager.glDeleteShader(this.id);
				LOGGER.debug("Destroyed compiled shader id={}", this.id);
				this.id = -1;
				this.references = 0;
				return true;
			}
			else
			{
				return false;
			}
		}
	}
}
