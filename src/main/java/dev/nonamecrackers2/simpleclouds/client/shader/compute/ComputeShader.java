package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.client.ForgeHooksClient;

public class ComputeShader implements AutoCloseable
{
	protected static final Logger LOGGER = LogManager.getLogger("simpleclouds/ComputeShader");
	private static final Pattern LOCAL_GROUP_REPLACER = Pattern.compile("\\$\\{.*?\\}");
	private static int maxGroupX = -1;
	private static int maxGroupY = -1;
	private static int maxGroupZ = -1;
	private static int maxLocalGroupX = -1;
	private static int maxLocalGroupY = -1;
	private static int maxLocalGroupZ = -1;
	private static int maxLocalInvocations = -1;
	
	private int id;
	private int shaderId;
	private final String name;
	private final Int2ObjectMap<BufferObject> buffers = new Int2ObjectOpenHashMap<>();
	
	private ComputeShader(int id, int shaderId, String name)
	{
		this.id = id;
		this.shaderId = shaderId;
		this.name = name;
	}
	
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		LOGGER.debug("Closing compute shader id={}", this.id);
		this.buffers.forEach((binding, buffer) -> {
			buffer.close();
		});
		this.buffers.clear();
		if (this.shaderId != -1)
		{
			GlStateManager.glDeleteShader(this.shaderId);
			this.shaderId = -1;
		}
		if (this.id != -1)
		{
			GlStateManager.glDeleteProgram(this.id);
			this.id = -1;
		}
	}
	
	public void forUniform(String name, IntConsumer consumer)
	{
		ProgramManager.glUseProgram(this.id);
		int loc = GlStateManager._glGetUniformLocation(this.id, name);
		if (loc == -1)
			LOGGER.warn("Could not find uniform with name '{}'", name);
		else
			consumer.accept(loc);
		ProgramManager.glUseProgram(0);
	}
	
	public ShaderStorageBuffer bindShaderStorageBuffer(int binding, int usage)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		if (this.buffers.containsKey(binding))
			throw new IllegalArgumentException("Buffer already binded!");
		int id = GlStateManager._glGenBuffers();
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding, id);
		return this.putBufferObject(binding, new ShaderStorageBuffer(id, binding, usage));
	}
	
	public AtomicCounter bindAtomicCounter(int binding, int usage)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		if (this.buffers.containsKey(binding))
			throw new IllegalArgumentException("Buffer already binded!");
		int id = GlStateManager._glGenBuffers();
		GL30.glBindBufferBase(GL42.GL_ATOMIC_COUNTER_BUFFER, binding, id);
		return this.putBufferObject(binding, new AtomicCounter(id, binding, usage));
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BufferObject> T getBufferObject(int binding)
	{
		RenderSystem.assertOnRenderThread();
		return (T)Objects.requireNonNull(this.buffers.get(binding), "Shader storage buffer with binding " + binding + " does not exist");
	}
	
	private <T extends BufferObject> T putBufferObject(int binding, T object)
	{
		this.buffers.put(binding, object);
		return object;
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
			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_ATOMIC_COUNTER_BARRIER_BIT);
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
		if (this.id == -1 || this.shaderId == -1)
			throw new IllegalStateException("Compute shader is no longer valid!");
	}
	
	public static ComputeShader loadShader(ResourceLocation loc, ResourceProvider provider, int localX, int localY, int localZ) throws IOException
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
		
		String path = "shaders/compute/" + loc.getPath() + ".csh";
		ResourceLocation finalLoc = new ResourceLocation(loc.getNamespace(), path);
		Resource resource = provider.getResourceOrThrow(finalLoc);
			
		try (InputStream inputStream = resource.open())
		{
			final String fullPath = FileUtil.getFullResourcePath(path);
			return compileShader(loc.toString(), inputStream, resource.sourcePackId(), new GlslPreprocessor()
			{
				private final Set<String> importedPaths = Sets.newHashSet();
	
				@Override
				public List<String> process(String file)
				{
					file = LOCAL_GROUP_REPLACER.matcher(file).replaceAll(result -> {
						String group = result.group();
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
		}
	}
	
	private static ComputeShader compileShader(String loc, InputStream inputStream, String packId, GlslPreprocessor preprocessor) throws IOException
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
		int programId = ProgramManager.createProgram();
		GlStateManager.glAttachShader(programId, shaderId);
		GlStateManager.glLinkProgram(programId);
		int i = GlStateManager.glGetProgrami(programId, GL20.GL_LINK_STATUS);
		if (i == 0)
		{
			LOGGER.warn("Error encountered when linking program containing computer shader {}. Log output:", loc);
			LOGGER.warn(GlStateManager.glGetProgramInfoLog(programId, GL11.GL_HINT_BIT));
		}
		return new ComputeShader(programId, shaderId, loc);
	}
}
