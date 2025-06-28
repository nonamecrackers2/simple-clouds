package dev.nonamecrackers2.simpleclouds.common.cloud.spawning.profiling;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class ProfilingCloudGenerator extends CloudGenerator
{
	public static final Logger LOGGER = LogManager.getLogger();
	private static final int ORIGIN_X = 0;
	private static final int ORIGIN_Z = 0;
	private static final List<SpawnRegion> DEFAULT = ImmutableList.of(new SpawnRegion(ORIGIN_X, ORIGIN_Z, SimpleCloudsConstants.SPAWN_RADIUS));
	private final ProfilingCloudGenerator.Results results = new ProfilingCloudGenerator.Results();
	
	public ProfilingCloudGenerator(CloudTypeSource cloudGetter, Supplier<CloudSpawningConfig> spawnConfig)
	{
		super(cloudGetter, spawnConfig);
	}

	@Override
	protected List<SpawnRegion> determineValidSpawnRegions(RandomSource random, Level level)
	{
		return DEFAULT;
	}
	
	@Override
	public void tick(Level level, float speed)
	{
		super.tick(level, speed);
		
		this.results.tick(this.getTotalCloudRegions(), this.getCloudAtWorldPosition(ORIGIN_X, ORIGIN_Z));
	}
	
	@Override
	public boolean addCloud(CloudRegion region, Order order)
	{
		if (!super.addCloud(region, order))
			return false;
		CloudType type = Objects.requireNonNull(this.cloudGetter.getCloudTypeForId(region.getCloudTypeId()), "Cloud type not provided");
		this.results.addEntry(region, type.weatherType());
		return true;
	}
	
	public static CompletableFuture<ProfilingCloudGenerator.Results> profile(CloudSpawningConfig config, CloudTypeSource getter, int iterations)
	{
		return CompletableFuture.supplyAsync(() -> 
		{
			ProfilingCloudGenerator generator = new ProfilingCloudGenerator(getter, () -> config);
			for (int i = 0; i < iterations; i++)
				generator.tick(null, 1.0F);
			return generator.results;
		});
	}
	
	public static class Results
	{
		private int totalCloudTypesGenerated;
		private int totalRainCloudsGenerated;
		private int totalThunderstormCloudsGenerated;
		private MinMax currentCloudCount = new MinMax();
		private final Map<ResourceLocation, CloudStats> cloudStats = Maps.newHashMap();
		private int totalTicksElapsed;
		private float averageSpawnTime;
		private float averageRainSpawnTime;
		private float averageThunderstormSpawnTime;
		
		private Results() {}
		
		public void tick(int totalCurrentCount, @Nullable CloudRegion atOrigin)
		{
			this.totalTicksElapsed++;
			this.currentCloudCount.addEntry(totalCurrentCount);
			
			for (var entry : this.cloudStats.entrySet())
			{
				ResourceLocation id = entry.getKey();
				boolean isOverPlayer = false;
				if (atOrigin != null && id.equals(atOrigin.getCloudTypeId()))
					isOverPlayer = true;
				entry.getValue().tick(isOverPlayer);
			}
		}
		
		public void addEntry(CloudRegion region, WeatherType type)
		{
			this.totalCloudTypesGenerated++;
			this.averageSpawnTime = (float)this.totalTicksElapsed / (float)this.totalCloudTypesGenerated;
			if (type.includesRain())
			{
				this.totalRainCloudsGenerated++;
				this.averageRainSpawnTime = (float)this.totalTicksElapsed / (float)this.totalRainCloudsGenerated; 
			}
			if (type.includesThunder())
			{
				this.totalThunderstormCloudsGenerated++;
				this.averageThunderstormSpawnTime = (float)this.totalTicksElapsed / (float)this.totalThunderstormCloudsGenerated;
			}
			
			CloudStats stats = this.cloudStats.computeIfAbsent(region.getCloudTypeId(), r -> {
				return new CloudStats();
			});
			
			stats.addEntry(region, this.totalTicksElapsed);
		}
		
		public Map<ResourceLocation, CloudStats> getIndividualStats()
		{
			return this.cloudStats;
		}
		
		public MinMax getCurrentCloudCountStats()
		{
			return this.currentCloudCount;
		}
		
		public int getTotalCloudTypesGenerated()
		{
			return this.totalCloudTypesGenerated;
		}
		
		public int getTotalTicksElapsed()
		{
			return this.totalTicksElapsed;
		}
		
		public float getAverageThunderstormSpawnTime()
		{
			return this.averageThunderstormSpawnTime;
		}
		
		public float getAverageRainSpawnTime()
		{
			return this.averageRainSpawnTime;
		}
		
		public float getAverageSpawnTime()
		{
			return this.averageSpawnTime;
		}
	}
	
	public static class CloudStats
	{
		private int totalSpawned;
		private boolean wasOverPlayer;
		private int ticksOverPlayer;
		private float averageTicksToSpawn;
		private MinMax timeOverPlayer = new MinMax();
		private MinMax speed = new MinMax();
		private MinMax radius = new MinMax();
		private MinMax stretchFactor = new MinMax();
		private MinMax existTicks = new MinMax();
		private MinMax growTicks = new MinMax();
		
		private CloudStats() {}
		
		public void tick(boolean isOverPlayer)
		{
			if (isOverPlayer)
			{
				this.ticksOverPlayer++;
				this.wasOverPlayer = true;
			}
			else if (this.wasOverPlayer)
			{
				this.timeOverPlayer.addEntry(this.ticksOverPlayer);
				this.ticksOverPlayer = 0;
				this.wasOverPlayer = false;
			}
		}
		
		public void addEntry(CloudRegion region, int totalTicksElapsed)
		{
			this.totalSpawned += 1;
			
			this.averageTicksToSpawn = (float)totalTicksElapsed / (float)this.totalSpawned;
			
			this.speed.addEntry(region.getMaxSpeed());
			this.radius.addEntry(region.getInitialWorldRadius());
			this.stretchFactor.addEntry(region.getStretch());
			this.existTicks.addEntry(region.getExistForTicks());
			this.growTicks.addEntry(region.getGrowTicks());
		}
		
		public int getTotalSpawned()
		{
			return this.totalSpawned;
		}
		
		public float getAverageTicksToSpawn()
		{
			return this.averageTicksToSpawn;
		}
		
		public MinMax getTimeOverPlayer()
		{
			return this.timeOverPlayer;
		}
		
		public MinMax getSpeedStats()
		{
			return this.speed;
		}
		
		public MinMax getRadiusStats()
		{
			return this.radius;
		}
		
		public MinMax getStretchFactorStats()
		{
			return this.stretchFactor;
		}

		public MinMax getExistTicks()
		{
			return this.existTicks;
		}
		
		public MinMax getGrowTicks()
		{
			return this.growTicks;
		}
	}
	
	public static class MinMax
	{
		private float min;
		private float max;
		private float sum;
		private int count;
		private float avg;
		
		private MinMax() {}
		
		public void addEntry(float value)
		{
			if (this.count == 0)
			{
				this.min = value;
				this.max = value;
				this.sum = value;
				this.count = 1;
				this.avg = value;
				return;
			}
			
			if (value < this.min)
				this.min = value;
			
			if (value > this.max)
				this.max = value;
			
			this.sum += value;
			this.count += 1;
			
			this.avg = this.sum / (float)this.count;
		}
		
		public int getCount()
		{
			return this.count;
		}
		
		public float getMin()
		{
			return this.min;
		}
		
		public float getMax()
		{
			return this.max;
		}
		
		public float getAvg()
		{
			return this.avg;
		}
	}
}
