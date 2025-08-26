package dev.nonamecrackers2.simpleclouds.common.api;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.region.ScAPICloudRegion;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.ScAPICloudGenerator;
import dev.nonamecrackers2.simpleclouds.api.common.world.ScAPISpawnRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;

public interface ScAPICloudGeneratorImplHelper extends ScAPICloudGenerator
{
	@SuppressWarnings("unchecked")
	@Override
	default void api_setClouds(Collection<? extends ScAPICloudRegion> clouds)
	{
		this.setClouds((List<CloudRegion>)clouds);
	}
	
	@Override
	default List<? extends ScAPICloudRegion> api_getCloudsInRegion(ScAPISpawnRegion region)
	{
		return this.getCloudsInRegion((SpawnRegion)region);
	}
	
	@Override
	default List<? extends ScAPISpawnRegion> api_getRegionsThatOccupyCloud(ScAPICloudRegion cloud)
	{
		return this.getRegionsThatOccupyCloud((CloudRegion)cloud);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	default boolean api_removeClouds(Predicate<? extends ScAPICloudRegion> predicate)
	{
		return this.removeClouds((Predicate<CloudRegion>)predicate);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	default int api_removeCloudsCount(Predicate<? extends ScAPICloudRegion> predicate)
	{
		return this.removeCloudsCount((Predicate<CloudRegion>)predicate);
	}
	
	@Override
	default boolean addCloudToTop(ScAPICloudRegion region)
	{
		return this.addCloud((CloudRegion)region, CloudGenerator.Order.TOP);
	}
	
	@Override
	default boolean addCloudToBottom(ScAPICloudRegion region)
	{
		return this.addCloud((CloudRegion)region, CloudGenerator.Order.BOTTOM);
	}
	
	@Override
	default boolean addCloudUsingWeight(ScAPICloudRegion region)
	{
		return this.addCloud((CloudRegion)region, CloudGenerator.Order.USE_WEIGHT);
	}
	
	void setClouds(Collection<CloudRegion> clouds);
	
	List<CloudRegion> getCloudsInRegion(SpawnRegion region);
	
	List<SpawnRegion> getRegionsThatOccupyCloud(CloudRegion cloud);
	
	boolean removeClouds(Predicate<CloudRegion> predicate);
	
	int removeCloudsCount(Predicate<CloudRegion> predicate);
	
	boolean addCloud(CloudRegion region, CloudGenerator.Order order);
}
