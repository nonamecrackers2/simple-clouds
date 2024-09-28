package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.init.RegionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public abstract class CloudManager<T extends Level> implements CloudTypeSource
{
	public static final int CLOUD_HEIGHT_MAX = 2048;
	public static final int CLOUD_HEIGHT_MIN = 0;
	public static final int UPDATE_INTERVAL = 200;
	public static final float RANDOM_SPREAD = 10000.0F;
	protected final T level;
	protected final CloudTypeSource cloudSource;
	private RegionType regionGenerator = RegionTypes.VORONOI_DIAGRAM.get();
	private long seed;
	protected @Nullable RandomSource random;
	protected float scrollXO;
	protected float scrollYO;
	protected float scrollZO;
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
	protected Vector3f direction = new Vector3f(1.0F, 0.0F, 0.0F);
	protected float speed = 1.0F;
	protected int cloudHeight = 128;
	protected int tickCount;
	protected int nextLightningStrike = 60;
	protected boolean useVanillaWeather;

	@SuppressWarnings("unchecked")
	public static <T extends Level> CloudManager<T> get(T level)
	{
		return Objects.requireNonNull(((CloudManagerAccessor<T>)level).getCloudManager(), "Cloud manager is not available, this shouldn't happen!");
	}
	
	public CloudManager(T level, CloudTypeSource source)
	{
		this.level = level;
		this.cloudSource = source;
	}
	
	@Override
	public CloudType getCloudTypeForId(ResourceLocation id)
	{
		return this.cloudSource.getCloudTypeForId(id);
	}
	
	@Override
	public CloudType[] getIndexedCloudTypes()
	{
		return this.cloudSource.getIndexedCloudTypes();
	}
	
	public Pair<CloudType, Float> getCloudTypeAtPosition(float x, float z)
	{
		if (this.getCloudMode() != CloudMode.SINGLE)
		{
			CloudType[] types = this.getIndexedCloudTypes();
			float posX = this.scrollX + x / (float)SimpleCloudsConstants.CLOUD_SCALE;
			float posZ = this.scrollZ + z / (float)SimpleCloudsConstants.CLOUD_SCALE;
			var result = this.getRegionGenerator().getCloudTypeIndexAt(posX, posZ, SimpleCloudsConstants.REGION_SCALE, types.length);
			if (result.index() < 0 || result.index() >= types.length)
				throw new IndexOutOfBoundsException("Region type generator sent an invalid index: " + result.index());
			return Pair.of(types[result.index()], result.fade());
		}
		else
		{
			String rawId = this.getSingleModeCloudTypeRawId();
			ResourceLocation id = ResourceLocation.tryParse(rawId);
			if (id != null)
			{
				CloudType type = this.getCloudTypeForId(id);
				if (type != null)
					return Pair.of(type, 0.0F);
			}
			return Pair.of(SimpleCloudsConstants.FALLBACK, 0.0F);
		}
	}
	
	public boolean isRainingAt(BlockPos pos)
	{
		if (!this.level.canSeeSky(pos) || this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY())
			return false;
		
		if (this.level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.RAIN)
			return false;
		
		var info = this.getCloudTypeAtPosition((float)pos.getX() + 0.5F, (float)pos.getZ() + 0.5F);
		CloudType type = info.getLeft();
		if ((float)pos.getY() + 0.5F > type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + 128.0F)
			return false;
		
		if (info.getLeft().weatherType().includesRain() && info.getRight() < SimpleCloudsConstants.RAIN_THRESHOLD - 0.01F)
			return true;
		else
			return false;
	}
	
	public float getRainLevel(float x, float y, float z)
	{
		var info = this.getCloudTypeAtPosition(x, z);
		CloudType type = info.getLeft();
		
		if (!type.weatherType().includesRain())
			return 0.0F;
		
		float fade = info.getRight();
		float verticalFade = 1.0F - Mth.clamp((y - (type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + this.getCloudHeight())) / SimpleCloudsConstants.RAIN_VERTICAL_FADE, 0.0F, 1.0F);
		return Math.min(1.0F, Math.max(0.0F, SimpleCloudsConstants.RAIN_THRESHOLD - fade) / SimpleCloudsConstants.RAIN_FADE) * verticalFade;
	}
	
	public void setRegionGenerator(RegionType type)
	{
		this.regionGenerator = type;
	}
	
	public RegionType getRegionGenerator()
	{
		return this.regionGenerator;
	}

	public void init(long seed)
	{
		RandomSource random = this.setSeed(seed);
		this.random = random;
		this.direction = new Vector3f(random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F, random.nextFloat() * 2.0F - 1.0F).normalize();
		this.scrollX = (random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPREAD;
		this.scrollY = (random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPREAD;
		this.scrollZ = (random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPREAD;
		this.speed = 1.0F;
	}
	
	public int getCloudHeight()
	{
		return this.cloudHeight;
	}
	
	public void setCloudHeight(int height)
	{
		this.cloudHeight = height;
	}

	public void tick()
	{
		this.tickCount++;
		
		this.scrollXO = this.scrollX;
		this.scrollYO = this.scrollY;
		this.scrollZO = this.scrollZ;
		float speed = this.getSpeed() * 0.01F;
		this.scrollX -= this.getDirection().x() * speed;
		this.scrollY -= this.getDirection().y() * speed;
		this.scrollZ -= this.getDirection().z() * speed;
		
		boolean flag = this.determineUseVanillaWeather();
		if (flag != this.useVanillaWeather)
		{
			this.useVanillaWeather = flag;
			this.resetVanillaWeather();
		}
		
		if (!this.useVanillaWeather)
			this.tickLightning();
	}
	
	protected void resetVanillaWeather() {}
	
	protected void tickLightning()
	{
		if (this.nextLightningStrike <= 0 || --this.nextLightningStrike > 0)
			return;
		this.attemptToSpawnLightning();
		int minInterval = SimpleCloudsConfig.COMMON.lightningSpawnIntervalMin.get();
		int maxInterval = Math.max(minInterval, SimpleCloudsConfig.COMMON.lightningSpawnIntervalMax.get());
		this.nextLightningStrike = Mth.randomBetweenInclusive(this.random, minInterval, maxInterval);
	}
	
	protected boolean determineUseVanillaWeather()
	{
		return useVanillaWeather(this.level, this);
	}
	
	public final boolean shouldUseVanillaWeather()
	{
		return this.useVanillaWeather;
	}
	
	protected abstract void attemptToSpawnLightning();
	
	protected abstract void spawnLightning(CloudType type, float fade, int x, int z, boolean soundOnly);
	
	public abstract CloudMode getCloudMode();
	
	public abstract String getSingleModeCloudTypeRawId();
	
	public void spawnLightning(int x, int z, boolean soundOnly)
	{
		var info = this.getCloudTypeAtPosition((float)x + 0.5F, (float)z + 0.5f);
		this.spawnLightning(info.getLeft(), info.getRight(), x, z, soundOnly);
	}
	
	public int getTickCount()
	{
		return this.tickCount;
	}
	
	public long getSeed()
	{
		return this.seed;
	}
	
	public RandomSource setSeed(long seed)
	{
		this.seed = seed;
		return RandomSource.create(seed);
	}
	
	public Vector3f getDirection()
	{
		return this.direction;
	}
	
	public void setDirection(@Nonnull Vector3f direction)
	{
		this.direction = new Vector3f(Objects.requireNonNull(direction)).normalize();
	}
	
	public float getSpeed()
	{
		return this.speed;
	}
	
	public void setSpeed(float speed)
	{
		this.speed = Math.max(0.0F, speed);
	}

	public float getScrollX()
	{
		return this.scrollX;
	}

	public void setScrollX(float scrollX)
	{
		this.scrollX = scrollX;
		this.scrollXO = scrollX;
	}

	public float getScrollY()
	{
		return this.scrollY;
	}

	public void setScrollY(float scrollY)
	{
		this.scrollY = scrollY;
		this.scrollYO = scrollY;
	}

	public float getScrollZ()
	{
		return this.scrollZ;
	}

	public void setScrollZ(float scrollZ)
	{
		this.scrollZ = scrollZ;
		this.scrollZO = scrollZ;
	}
	
	public float getScrollX(float partialTicks)
	{
		return Mth.lerp(partialTicks, this.scrollXO, this.scrollX);
	}
	
	public float getScrollY(float partialTicks)
	{
		return Mth.lerp(partialTicks, this.scrollYO, this.scrollY);
	}
	
	public float getScrollZ(float partialTicks)
	{
		return Mth.lerp(partialTicks, this.scrollZO, this.scrollZ);
	}
	
	public static boolean isValidLightning(CloudType type, float fade, RandomSource random)
	{
		return type.weatherType().includesThunder() && fade < 0.8F;// && (fade > 0.7F || random.nextInt(3) == 0); 
	}
	
	public static boolean useVanillaWeather(Level level, CloudTypeSource source)
	{
		if (!SimpleCloudsConfig.SERVER_SPEC.isLoaded())
			return false;
		
		boolean flag = SimpleCloudsConfig.SERVER.dimensionWhitelist.get().stream().anyMatch(val -> {
			return level.dimension().location().toString().equals(val);
		});
		
		if (SimpleCloudsConfig.SERVER.whitelistAsBlacklist.get() ? flag : !flag)
			return true;
		
		CloudMode mode = SimpleCloudsConfig.SERVER.cloudMode.get();
		
		switch (mode)
		{
		case AMBIENT:
		{
			return true;
		}
		case SINGLE:
		{
			String rawId = SimpleCloudsConfig.SERVER.singleModeCloudType.get();
			ResourceLocation id = ResourceLocation.tryParse(rawId);
			if (id != null)
			{
				CloudType type = source.getCloudTypeForId(id);
				if (type != null && type.weatherType() == WeatherType.NONE)
					return true;
			}
		}
		default:
		{
			return false;
		}
		}
	}
}
