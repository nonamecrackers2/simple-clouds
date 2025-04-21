package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

//TODO: Test extensively on multiplayer
public class ServerCloudGenerator extends CloudGenerator
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int AUTO_SYNC_INTERVAL = 240;
	private int syncTimer = AUTO_SYNC_INTERVAL;
	private boolean requiresSync;
	
	public ServerCloudGenerator(CloudGetter getter, Supplier<CloudSpawningConfig> config)
	{
		super(getter, config);
	}
	
	public CompoundTag toTag()
	{
		CompoundTag tag = new CompoundTag();
		ListTag regions = new ListTag();
		for (CloudRegion region : this.getClouds())
			regions.add(region.toTag());
		tag.put("regions", regions);
		tag.putInt("ticks_till_next_gen", this.ticksTillNextGen);
		return tag;
	}
	
	public void readTag(CompoundTag tag)
	{
		ListTag regionsTag = tag.getList("regions", 10);
		List<CloudRegion> regions = Lists.newArrayList();
		for (int i = 0; i < regionsTag.size(); i++)
		{
			CompoundTag regionTag = regionsTag.getCompound(i);
			try {
				regions.add(new CloudRegion(regionTag));
			} catch (IllegalArgumentException e) {
				LOGGER.error("Failed to read cloud region: ", e);
			}
		}
		System.out.println("what is up, reading");
		System.out.println(regions);
		this.setClouds(regions);
		this.ticksTillNextGen = tag.getInt("ticks_till_next_gen");
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
	public boolean removeClouds(Predicate<CloudRegion> predicate)
	{
		if (!super.removeClouds(predicate))
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
		
		//if (level.dimension() == Level.OVERWORLD)
		//	System.out.println(this.ticksTillNextGen);
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
