package dev.nonamecrackers2.simpleclouds.common.api;

import org.apache.maven.artifact.versioning.ArtifactVersion;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.ScAPIInternal;
import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.api.common.ScAPIHooks;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.region.ScAPICloudRegion;
import dev.nonamecrackers2.simpleclouds.api.common.world.ScAPICloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class SimpleCloudsAPIImpl implements SimpleCloudsAPI
{
	public static final SimpleCloudsAPIImpl INSTANCE;
	private final SimpleCloudsHooks hooks = new SimpleCloudsHooks();
	
	static 
	{
		INSTANCE = new SimpleCloudsAPIImpl();
		ScAPIInternal._setApi(INSTANCE);
	}
	
	public static void bootstrap() {}
	
	@Override
	public ArtifactVersion getSimpleCloudsVersion()
	{
		return SimpleCloudsMod.getModVersion();
	}
	
	@Override
	public ScAPICloudManager getCloudManager(Level level)
	{
		return CloudManager.get(level);
	}
	
	@Override
	public ScAPIHooks getHooks()
	{
		return this.hooks;
	}
	
	@Override
	public ScAPICloudRegion createCloudRegion(ResourceLocation cloudTypeId, Vec2 movementDirection, float maxSpeed, float accelerationFactor, float posX, float posZ, float radius, float rotation, float stretchFactor, int existsForTicks, int growTicks, int orderWeight)
	{
		return new CloudRegion(cloudTypeId, movementDirection, maxSpeed, accelerationFactor, posX, posZ, radius, rotation, stretchFactor, existsForTicks, growTicks, orderWeight);
	}
}
