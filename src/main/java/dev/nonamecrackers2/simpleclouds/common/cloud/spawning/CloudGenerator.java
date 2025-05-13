package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.common.api.ScAPICloudGeneratorImplHelper;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public abstract class CloudGenerator implements ScAPICloudGeneratorImplHelper
{
	public static final int SPAWN_RADIUS = 10000; 
	public static final int SPAWN_ATTEMPTS = 10; 
	public static final float MIN_SPAWN_DIST_BETWEEN_REGIONS = 500.0F; 
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudGenerator");
	private List<SpawnRegion> spawnRegions = Lists.newArrayList();
	private final List<CloudRegion> clouds = Lists.newArrayList();
	protected final RandomSource random;
	protected final Supplier<CloudSpawningConfig> spawnConfig;
	protected int ticksTillNextGen;
	protected final CloudGetter cloudGetter;
	
	public CloudGenerator(CloudGetter cloudGetter, Supplier<CloudSpawningConfig> spawnConfig)
	{
		this.random = RandomSource.create();
		this.cloudGetter = cloudGetter;
		this.spawnConfig = spawnConfig;
	}
	
	@Override
	public final List<CloudRegion> getClouds()
	{
		return ImmutableList.copyOf(this.clouds);
	}
	
	@Override
	public final List<SpawnRegion> getSpawnRegions()
	{
		return ImmutableList.copyOf(this.spawnRegions);
	}
	
	@Override
	public List<CloudRegion> getCloudsInRegion(SpawnRegion region)
	{
		List<CloudRegion> clouds = Lists.newArrayList();
		for (CloudRegion cloud : this.clouds)
		{
			if (cloud.intersects(region))
				clouds.add(cloud);
		}
		return clouds;
	}
	
	public @Nullable CloudRegion getCloudAtWorldPosition(float worldX, float worldZ)
	{
		return this.getCloudAtPosition(worldX / (float)SimpleCloudsConstants.CLOUD_SCALE, worldZ / (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	public @Nullable CloudRegion getCloudAtPosition(float x, float z) //TODO: Add to API
	{
		return CloudRegion.calculateAt(this.getClouds(), x, z).getLeft();
	}
	
	@Override
	public List<SpawnRegion> getRegionsThatOccupyCloud(CloudRegion cloud)
	{
		List<SpawnRegion> regions = Lists.newArrayList();
		for (SpawnRegion region : this.spawnRegions)
		{
			if (cloud.intersects(region))
				regions.add(region);
		}
		return regions;
	}
	
	@Override
	public final int getTotalCloudRegions()
	{
		return this.clouds.size();
	}
	
	@Override
	public void setClouds(Collection<CloudRegion> clouds)
	{
		this.removeAllClouds();
		clouds.forEach(r -> {
			this.clouds.add(r);
		});
	}
	
	@Override
	public boolean removeAllClouds()
	{
		return this.removeClouds(r -> true);
	}
	
	@Override
	public boolean removeClouds(Predicate<CloudRegion> predicate)
	{
		boolean anyPassed = false;
		var iterator = this.clouds.iterator();
		while (iterator.hasNext())
		{
			CloudRegion region = iterator.next();
			if (predicate.test(region))
			{
				iterator.remove();
				anyPassed = true;
			}
		}
		return anyPassed;
	}
	
	@Override
	public boolean addCloud(CloudRegion region, CloudGenerator.Order order)
	{
		if (!this.cloudGetter.doesCloudTypeExist(region.getCloudTypeId()))
		{
			LOGGER.warn("Attempted to spawn a cloud formation: unknown id '{}'", region.getCloudTypeId());
			return false;
		}
		
		if (this.clouds.contains(region))
			return false;
		
		// Ensures we wont go over the maximum cloud formations for all regions that would include
		// this cloud formation
		for (SpawnRegion spawnRegion : this.getRegionsThatOccupyCloud(region))
		{
			int totalCount = 0;
			for (CloudRegion cloud : this.clouds)
			{
				if (cloud.intersects(spawnRegion))
					totalCount++;
			}
			if (totalCount >= SimpleCloudsConstants.MAX_CLOUD_FORMATIONS)
			{
				System.out.println("refusing cloud region, too many");
				return false;
			}
		}
		
		order.appender.accept(this.clouds, region);
		
		System.out.println(this.clouds.stream().map(CloudRegion::getOrderWeight).toList());
		
		return true;
	}
	
	public void initialize(RandomSource random, Level level)
	{
		this.spawnRegions = this.determineValidSpawnRegions(random, level);
		this.removeAllClouds();
		CloudSpawningConfig config = this.spawnConfig.get();
		this.ticksTillNextGen = config.getSpawnInterval().sample(random);
	}
	
	public void tick(Level level)
	{
		this.spawnRegions = this.determineValidSpawnRegions(this.random, level);
		
		var iterator = this.clouds.iterator();
		while (iterator.hasNext())
		{
			CloudRegion region = iterator.next();
			
			//NOTE: If a cloud formation (region) is on the edge of a spawn region and is not visible, if the player moves even a slightly bit they can make that
			//formation visible again causing it to tick. It could then move outside the region again, then the player can move and make it become
			//visible again causing a cycle. This shouldn't happen as often since cloud formations shrink and will shrink extra fast when no longer visible,
			//making it so they will shrink farther away from the edge of a spawn region preventing this, but it is behavior to note
			//TODO: Account for stretch
			boolean isVisible = SpawnRegion.doesCircleIntersect(this.spawnRegions, region.getWorldX(), region.getWorldZ(), region.getWorldRadius() + (float)SimpleCloudsConstants.CLOUD_SCALE / SimpleCloudsConstants.REGION_EDGE_FADE_FACTOR);
			if (isVisible != region.wasPriorVisible())
				this.onRegionVisibilityChange(region, isVisible);
			region.tick(this.random, level, isVisible);
			
			if (!this.cloudGetter.doesCloudTypeExist(region.getCloudTypeId()))
			{
				LOGGER.warn("Cloud type with id {} no longer exists, removing cloud region", region.getCloudTypeId());
				iterator.remove();
			}
			
			if (region.isDead())
			{
				iterator.remove();
				if (!level.isClientSide)
					System.out.println("cloud region died, was visible: " + isVisible + ", total: " + this.getTotalCloudRegions());
			}
		}
		
		if (this.ticksTillNextGen > 0)
			this.ticksTillNextGen--;
		
		CloudSpawningConfig config = this.spawnConfig.get();
		
		// In case the spawning config changes and the spawn interval is very different
		int maxSpawnInterval = config.getSpawnInterval().getMaxValue();
		if (this.ticksTillNextGen > maxSpawnInterval)
			this.ticksTillNextGen = maxSpawnInterval;
		
		if (!config.isEmpty() && this.shouldGenerateCloud(config, this.random, level))
			this.spawnCloud(config, level);
	}
	
	protected boolean shouldGenerateCloud(CloudSpawningConfig config, RandomSource random, Level level)
	{
		return this.ticksTillNextGen <= 0;
	}
	
	protected void spawnCloud(CloudSpawningConfig config, Level level)
	{
		this.ticksTillNextGen = config.getSpawnInterval().sample(this.random);
		System.out.println("next spawn attempt: " + this.ticksTillNextGen);
		
		SpawnRegion.randomPointForEachRegion(this.spawnRegions, this.random, SPAWN_ATTEMPTS, (r, p) -> 
		{
			if (this.getCloudsInRegion(r).size() >= config.getMaxRegions())
				return true;
			
			float x = (float)p.x + 0.5F;
			float z = (float)p.y + 0.5F;
			
			return this.createRandomRegion(config, (float)r.x() + 0.5F, (float)r.z() + 0.5F, x, z, this.random, true).map(region -> {
				return this.addCloud(region, CloudGenerator.Order.USE_WEIGHT);
			}).orElse(false);
		});
	}
	
	protected Optional<CloudRegion> createRandomRegion(CloudSpawningConfig config, float playerX, float playerZ, float x, float z, RandomSource random, boolean growTime)
	{
		for (CloudRegion region : this.getClouds())
		{
			float dist = Vector2f.distance(x, z, region.getWorldX(), region.getWorldZ()) - region.getWorldRadius();
			if (dist <= MIN_SPAWN_DIST_BETWEEN_REGIONS)
				return Optional.empty();
		}
		
		CloudSpawningConfig.Info info = config.getRandom(random).orElse(null);
		if (info == null)
			return Optional.empty();
		
		CloudType type = this.cloudGetter.getCloudTypeForId(info.cloudType());
		if (type == null)
		{
			LOGGER.warn("Spawn config has unknown cloud type with id '{}'", info.cloudType());
			return Optional.empty();
		}
		
		Vec2 direction;
		float deltaAdj = info.movesToPlayer() ? 0.1F : 1.0F;
		float deltaX = (playerX - x) * (1.0F + random.nextFloat() * deltaAdj);
		float deltaZ = (playerZ - z) * (1.0F + random.nextFloat() * deltaAdj);
		float rotation = (float)Math.atan2(deltaX, deltaZ) + (float)Math.PI;
		if (random.nextInt(5) == 0)
			direction = new Vec2(random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F).normalized();
		else
			direction = new Vec2(deltaX, deltaZ).normalized();
		
		float radius = (float)info.radius().sample(random);
		float maxSpeed = 0.1F;
		float accelerationFactor = 0.01F;
		int existTicks = info.existTicks().sample(random);
		int growTicks = growTime ? Math.min(existTicks, info.growTicks().sample(random)) : 0;
		float stretchFactor = info.stretchFactor().sample(random);
		
		return Optional.of(new CloudRegion(info.cloudType(), direction, maxSpeed, accelerationFactor, x / (float)SimpleCloudsConstants.CLOUD_SCALE, z / (float)SimpleCloudsConstants.CLOUD_SCALE, radius / (float)SimpleCloudsConstants.CLOUD_SCALE, rotation, stretchFactor, existTicks, growTicks, info.orderWeight()));
	}
	
	public void doInitialGen(int x, int z, Level level, boolean ignoreOtherRegions)
	{
		SpawnRegion region = new SpawnRegion(x, z, CloudGenerator.SPAWN_RADIUS);
		
		CloudSpawningConfig config = this.spawnConfig.get();
		
		if (this.getCloudsInRegion(region).size() > config.getMaxInitialRegions())
			return;
		
		for (int i = 0; i < config.getMaxInitialRegions(); i++)
		{
			for (int j = 0; j < SPAWN_ATTEMPTS; j++)
			{
				Vector2i pos = SpawnRegion.getRandomPointInRegion(region, this.random);
				if (!ignoreOtherRegions && this.spawnRegions.stream().anyMatch(r -> r.includesPoint(pos.x, pos.y)))
					continue;
				CloudRegion cloudFormation = this.createRandomRegion(config, (float)x + 0.5F, (float)z + 0.5F, (float)pos.x + 0.5F, (float)pos.y + 0.5F, this.random, false).orElse(null);
				if (cloudFormation == null)
					continue;
				this.addCloud(cloudFormation, CloudGenerator.Order.USE_WEIGHT);
				break;
			}
		}
	}
	
	protected void onRegionVisibilityChange(CloudRegion region, boolean nowVisible) {}
	
	protected abstract List<SpawnRegion> determineValidSpawnRegions(RandomSource random, Level level);
	
	public static enum Order
	{
		TOP((l, r) -> 
		{
			l.add(r);
		}),
		BOTTOM((l, r) -> 
		{
			l.add(0, r);
		}),
		USE_WEIGHT((l, r) -> 
		{
			int prevWeight = 0;
			for (int i = 0; i < l.size(); i++)
			{
				CloudRegion region = l.get(i);
				if (r.getOrderWeight() >= prevWeight && r.getOrderWeight() <= region.getOrderWeight())
				{
					l.add(i, r);
					prevWeight = region.getOrderWeight();
					return;
				}
			}
			l.add(r);
		});
		
		private final BiConsumer<List<CloudRegion>, CloudRegion> appender;
		
		private Order(BiConsumer<List<CloudRegion>, CloudRegion> appender)
		{
			this.appender = appender;
		}
	}
}
