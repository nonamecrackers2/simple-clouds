package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.CreateRegionFunction;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.SpawnInfo;
import dev.nonamecrackers2.simpleclouds.api.common.event.CloudRegionNaturallySpawnEvent;
import dev.nonamecrackers2.simpleclouds.api.common.event.CloudRegionRemovedEvent;
import dev.nonamecrackers2.simpleclouds.common.api.ScAPICloudGeneratorImplHelper;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.common.MinecraftForge;

public abstract class CloudGenerator implements ScAPICloudGeneratorImplHelper
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudGenerator");
	private List<SpawnRegion> spawnRegions = Lists.newArrayList();
	private final List<CloudRegion> clouds = Lists.newArrayList();
	protected RandomSource random = RandomSource.create();
	protected final Supplier<CloudSpawningConfig> spawnConfig;
	protected int ticksTillNextGen;
	protected final CloudTypeSource cloudGetter;
	
	public CloudGenerator(CloudTypeSource cloudGetter, Supplier<CloudSpawningConfig> spawnConfig)
	{
		this.cloudGetter = cloudGetter;
		this.spawnConfig = spawnConfig;
	}

	public int getTicksTillNextGen()
	{
		return this.ticksTillNextGen;
	}
	
	public Supplier<CloudSpawningConfig> getSpawnConfig()
	{
		return this.spawnConfig;
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
	
	@Override
	public @Nullable CloudRegion getCloudAtWorldPosition(float worldX, float worldZ)
	{
		return this.getCloudAtPosition(worldX / (float)SimpleCloudsConstants.CLOUD_SCALE, worldZ / (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	@Override
	public @Nullable CloudRegion getCloudAtPosition(float x, float z) 
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
		return this.removeCloudsCount(predicate) > 0;
	}
	
	@Override
	public int removeCloudsCount(Predicate<CloudRegion> predicate)
	{
		int count = 0;
		var iterator = this.clouds.iterator();
		while (iterator.hasNext())
		{
			CloudRegion region = iterator.next();
			if (predicate.test(region))
			{
				iterator.remove();
				MinecraftForge.EVENT_BUS.post(new CloudRegionRemovedEvent(null, region, CloudRegionRemovedEvent.Reason.MANUALLY));
				count++;
			}
		}
		return count;
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
//				System.out.println("refusing cloud region, too many");
				return false;
			}
		}
		
		order.appender.accept(this.clouds, region);
		
//		System.out.println(this.clouds.stream().map(CloudRegion::getOrderWeight).toList());
		
		return true;
	}
	
	public void initialize(RandomSource random, Level level)
	{
		this.random = RandomSource.create();
		this.spawnRegions = this.determineValidSpawnRegions(this.random, level);
		this.removeAllClouds();
		CloudSpawningConfig config = this.spawnConfig.get();
		this.ticksTillNextGen = config.getSpawnInterval().sample(this.random);
	}
	
	public void tick(@Nullable Level level, float speed)
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
			boolean isVisible = SpawnRegion.doesCircleIntersect(this.spawnRegions, region.getWorldX(), region.getWorldZ(), region.getWorldRadius() / region.getStretch() + (float)SimpleCloudsConstants.CLOUD_SCALE / SimpleCloudsConstants.REGION_EDGE_FADE_FACTOR);
			if (isVisible != region.wasPriorVisible())
				this.onRegionVisibilityChange(region, isVisible);
			region.tick(this.random, level, isVisible, speed);
			
			if (!this.cloudGetter.doesCloudTypeExist(region.getCloudTypeId()))
			{
				LOGGER.warn("Cloud type with id {} no longer exists, removing cloud region", region.getCloudTypeId());
				iterator.remove();
				MinecraftForge.EVENT_BUS.post(new CloudRegionRemovedEvent(level, region, CloudRegionRemovedEvent.Reason.CLOUD_TYPE_NO_LONGER_EXISTS));
			}
			
			if (region.isDead())
			{
				iterator.remove();
				CloudRegionRemovedEvent.Reason reason = CloudRegionRemovedEvent.Reason.NATURALLY;
				if (!region.wasPriorVisible())
					reason = CloudRegionRemovedEvent.Reason.NO_LONGER_VISIBLE;
				MinecraftForge.EVENT_BUS.post(new CloudRegionRemovedEvent(level, region, reason));
//				if (level != null && !level.isClientSide)
//					System.out.println("cloud region died, was visible: " + isVisible + ", total: " + this.getTotalCloudRegions());
			}
		}
		
		if (this.ticksTillNextGen > 0)
			this.ticksTillNextGen -= Math.max(1, Mth.ceil(speed));
		
		CloudSpawningConfig config = this.spawnConfig.get();
		
		// In case the spawning config changes and the spawn interval is very different
		int maxSpawnInterval = config.getSpawnInterval().getMaxValue();
		if (this.ticksTillNextGen > maxSpawnInterval)
			this.ticksTillNextGen = maxSpawnInterval;
        
		if (!SimpleCloudsAPI.getApi().getHooks().isExternalWeatherControlEnabled())
        {
		    if (!config.isEmpty() && this.shouldGenerateCloud(config, this.random, level))
			    this.spawnCloud(config, level);
        }
	}
	
	protected boolean shouldGenerateCloud(CloudSpawningConfig config, RandomSource random, Level level)
	{
		return this.ticksTillNextGen <= 0;
	}
	
	public Optional<CloudRegion> spawnCloud(CloudSpawningConfig config, Level level)
	{
		return this.spawnCloud(() -> config.getRandom(this.random).orElse(null), config.getSpawnInterval().sample(this.random), config.getMaxRegions(), level);
	}
	
	@Override
	public Optional<CloudRegion> spawnCloud(Supplier<SpawnInfo> infoGetter, int nextSpawnInterval, int maxRegions, Level level)
	{
		return this.spawnCloud(infoGetter, nextSpawnInterval, maxRegions, level, this::createRegion);
	}
	
	@Override
	public Optional<CloudRegion> spawnCloud(Supplier<SpawnInfo> infoGetter, int nextSpawnInterval, int maxRegions, Level level, CreateRegionFunction regionFunc)
	{
		this.ticksTillNextGen = nextSpawnInterval;
//		System.out.println("next spawn attempt: " + this.ticksTillNextGen);
		
		MutableObject<CloudRegion> spawnedCloud = new MutableObject<>();
		
		SpawnRegion.randomPointForEachRegion(this.spawnRegions, this.random, SimpleCloudsConstants.SPAWN_ATTEMPTS, (r, p) -> 
		{
			if (this.getCloudsInRegion(r).size() >= maxRegions)
				return true;
			
			float x = (float)p.x + 0.5F;
			float z = (float)p.y + 0.5F;
			
			SpawnInfo info = infoGetter.get();
			
			if (info == null)
				return false;
			
			CloudType type = this.cloudGetter.getCloudTypeForId(info.cloudType());
			if (type == null)
			{
				LOGGER.warn("Spawn config has unknown cloud type with id '{}'", info.cloudType());
				return false;
			}
			
			return regionFunc.create(infoGetter.get(), (float)r.x() + 0.5F, (float)r.z() + 0.5F, x, z, this.random, true).map(apiRegion -> 
			{
				CloudRegion region = (CloudRegion)apiRegion;
				if (this.addCloud(region, CloudGenerator.Order.USE_WEIGHT))
				{
					spawnedCloud.setValue(region);
					MinecraftForge.EVENT_BUS.post(new CloudRegionNaturallySpawnEvent(level, apiRegion));
					return true;
				}
				else
				{
					return false;
				}
			}).orElse(false);
		});
		
		return Optional.ofNullable(spawnedCloud.getValue());
	}
	
	@Override
	public Optional<CloudRegion> createRegion(SpawnInfo info, float playerX, float playerZ, float x, float z, RandomSource random, boolean growTime)
	{
		for (CloudRegion region : this.getClouds())
		{
			float dist = Vector2f.distance(x, z, region.getWorldX(), region.getWorldZ()) - region.getWorldRadius();
			if (dist <= SimpleCloudsConstants.MIN_SPAWN_DIST_BETWEEN_REGIONS)
				return Optional.empty();
		}
		
		float deltaAdj = info.movesToPlayer() ? 0.1F : 1.0F;
		float deltaX = (playerX - x) * (1.0F + random.nextFloat() * deltaAdj);
		float deltaZ = (playerZ - z) * (1.0F + random.nextFloat() * deltaAdj);
		float rotation = (float)Math.atan2(deltaX, deltaZ) + (float)Math.PI;
		Vec2 direction;
		if (random.nextInt(5) == 0)
			direction = new Vec2(random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F).normalized();
		else
			direction = new Vec2(deltaX, deltaZ).normalized();
		
		float radius = (float)info.determineRadius(random);
		float maxSpeed = info.determineSpeed(random);
		float accelerationFactor = 0.01F;
		int existTicks = info.determineExistTicks(random);
		int growTicks = growTime ? info.determineGrowTicks(random) : 0;
		float stretchFactor = info.determineStretchFactor(random);
		
		return Optional.of(new CloudRegion(info.cloudType(), direction, maxSpeed, accelerationFactor, x / (float)SimpleCloudsConstants.CLOUD_SCALE, z / (float)SimpleCloudsConstants.CLOUD_SCALE, radius / (float)SimpleCloudsConstants.CLOUD_SCALE, rotation, stretchFactor, existTicks, growTicks, info.orderWeight()));
	}
	
	public void doInitialGen(int x, int z, Level level, boolean ignoreOtherRegions)
	{
		SpawnRegion region = new SpawnRegion(x, z, SimpleCloudsConstants.SPAWN_RADIUS);
		
		CloudSpawningConfig config = this.spawnConfig.get();
		
		if (this.getCloudsInRegion(region).size() > config.getMaxInitialRegions())
			return;
		
		for (int i = 0; i < config.getMaxInitialRegions(); i++)
		{
			for (int j = 0; j < SimpleCloudsConstants.SPAWN_ATTEMPTS; j++)
			{
				Vector2i pos = SpawnRegion.getRandomPointInRegion(region, this.random);
				if (this.getCloudsInRegion(region).size() >= config.getMaxInitialRegions())
					continue;
				if (!ignoreOtherRegions && this.spawnRegions.stream().anyMatch(r -> r.includesPoint(pos.x, pos.y)))
					continue;
				CloudRegion cloudFormation = this.createRegion(config.getRandom(this.random).orElse(null), (float)x + 0.5F, (float)z + 0.5F, (float)pos.x + 0.5F, (float)pos.y + 0.5F, this.random, false).orElse(null);
				if (cloudFormation == null)
					continue;
				this.addCloud(cloudFormation, CloudGenerator.Order.USE_WEIGHT);
				break;
			}
		}
	}
	
	protected void onRegionVisibilityChange(CloudRegion region, boolean nowVisible) {}
	
	protected abstract List<SpawnRegion> determineValidSpawnRegions(RandomSource random, @Nullable Level level);
	
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
