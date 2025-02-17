package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public abstract class CloudGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudGenerator");
	private final List<CloudRegion> clouds = Lists.newArrayList();
	protected final int initialMaximumAmount;
	protected final CloudGetter cloudGetter;
	
	public CloudGenerator(CloudGetter cloudGetter, int initialMaximumAmount)
	{
		this.cloudGetter = cloudGetter;
		this.initialMaximumAmount = initialMaximumAmount;
	}
	
	public List<CloudRegion> getClouds()
	{
		return ImmutableList.copyOf(this.clouds);
	}
	
	public void setClouds(List<CloudRegion> clouds)
	{
		this.removeAllClouds();
		clouds.forEach(this::addCloud);
	}
	
	public boolean removeAllClouds()
	{
		if (!this.clouds.isEmpty())
		{
			this.clouds.clear();
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean addCloud(CloudRegion region)
	{
		if (!this.cloudGetter.doesCloudTypeExist(region.getCloudTypeId()))
		{
			LOGGER.warn("Attempted to spawn a cloud formation: unknown id '{}'", region.getCloudTypeId());
			return false;
		}
		
		if (this.clouds.size() < SimpleCloudsConstants.MAX_CLOUD_FORMATIONS)
		{
			this.clouds.add(region);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void initialize(RandomSource random, Level level)
	{
		this.removeAllClouds();
		for (int i = 0; i < this.initialMaximumAmount; i++)
			this.generateCloud(random, level, true);
	}
	
	public void tick(RandomSource random, Level level)
	{
		var iterator = this.clouds.iterator();
		while (iterator.hasNext())
		{
			CloudRegion region = iterator.next();
			
			region.tick(random, level);
			
			if (!this.cloudGetter.doesCloudTypeExist(region.getCloudTypeId()))
			{
				LOGGER.warn("Cloud type with id {} no longer exists, removing cloud region", region.getCloudTypeId());
				iterator.remove();
			}
			
			if (region.isDead())
				iterator.remove();
		}
	}
	
	protected abstract void generateCloud(RandomSource random, Level level, boolean initial);
}
