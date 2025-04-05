package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.List;
import java.util.function.Supplier;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

//TODO: Remove necessary scaling for cloud region positions?
//TODO: Only send required cloud regions to the client
//TODO: Custom spawn configs for each cloud type, data driven (with data gen)
public class ServerCloudGenerator extends CloudGenerator
{
	private static final int AUTO_SYNC_INTERVAL = 240;
	private int syncTimer = AUTO_SYNC_INTERVAL;
	private boolean requiresSync;
	
	public ServerCloudGenerator(CloudGetter getter, Supplier<CloudSpawningConfig> config)
	{
		super(getter, config);
	}
	
	public boolean checkAndResetSync()
	{
		boolean flag = this.requiresSync;
		this.requiresSync = false;
		return flag;
	}
	
	@Override
	public boolean addCloud(CloudRegion region, CloudGenerator.Order order)
	{
		System.out.println("total clouds: " + (this.getTotalCloudRegions()));
		if (!super.addCloud(region, order))
			return false;
		System.out.println("success! total clouds: " + (this.getTotalCloudRegions()));
		this.requiresSync = true;
		return true;
	}
	
	@Override
	public boolean removeAllClouds()
	{
		if (!super.removeAllClouds())
			return false;
		this.requiresSync = true;
		return true;
	}
	
	@Override
	public void tick(Level level)
	{
		super.tick(level);
		
		if (this.syncTimer > 0)
		{
			this.syncTimer--;
			if (this.syncTimer == 0)
			{
				this.requiresSync = true;
				this.syncTimer = AUTO_SYNC_INTERVAL;
			}
		}
	}
	
	@Override
	protected void onRegionVisibilityChange(CloudRegion region, boolean nowVisible)
	{
		System.out.println("visibility changed: visible? " + nowVisible);
		this.requiresSync = true;
	}
	
	@Override
	protected List<SpawnRegion> determineValidSpawnRegions(RandomSource random, Level level)
	{
		return ServerCloudManager.regionsFromEntities(level.players(), SPAWN_RADIUS);
	}
}
