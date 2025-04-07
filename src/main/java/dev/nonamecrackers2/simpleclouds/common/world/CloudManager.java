package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2f;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public abstract class CloudManager<T extends Level> implements CloudGetter
{
	public static final int CLOUD_HEIGHT_MAX = 2048;
	public static final int CLOUD_HEIGHT_MIN = 0;
	public static final int UPDATE_INTERVAL = 200;
	public static final float RANDOM_SPREAD = 10000.0F;
	public static final float SCROLL_OFFSET = 100.0F;
	protected final T level;
	protected final CloudTypeSource cloudSource;
	protected final CloudGenerator cloudGenerator;
	private long seed;
	protected @Nullable RandomSource random;
	protected float scrollAngle;
	protected float scrollXO; //TODO: Make this periodic?
	protected float scrollYO;
	protected float scrollZO;
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
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
	
	public CloudManager(T level, CloudTypeSource source, Supplier<CloudSpawningConfig> configGetter, BiFunction<CloudGetter, Supplier<CloudSpawningConfig>, CloudGenerator> generatorFunc)
	{
		this.level = level;
		this.cloudSource = source;
		this.cloudGenerator = generatorFunc.apply(this, configGetter);
	}
	
	public CloudGenerator getCloudGenerator()
	{
		return this.cloudGenerator;
	}
	
	@Override
	public List<CloudRegion> getClouds()
	{
		return this.cloudGenerator.getClouds();
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
	
	public boolean isCloudGeneratorActive()
	{
		return this.getCloudMode() != CloudMode.SINGLE;
	}
	
	public void onPlayerJoin(Player player)
	{
		if (this.isCloudGeneratorActive())
			this.cloudGenerator.doInitialGen(player.getBlockX(), player.getBlockZ(), this.level);
	}
	
	@Override
	public Pair<CloudType, Float> getCloudTypeAtPosition(float x, float z)
	{
		if (this.getCloudMode() != CloudMode.SINGLE)
		{
			Pair<CloudRegion, Float> result = CloudRegion.calculateAt(this.getClouds(), x, z);
			CloudType type = null;
			if (result.getLeft() != null)
				type = this.getCloudTypeForId(result.getLeft().getCloudTypeId());
			if (type == null)
				type = SimpleCloudsConstants.EMPTY;
			return Pair.of(type, 1.0F - result.getRight());
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
			return Pair.of(SimpleCloudsConstants.EMPTY, 0.0F);
		}
	}
	
	public boolean isRainingAt(BlockPos pos)
	{
		if (!this.level.canSeeSky(pos) || this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY())
			return false;
		
		if (this.level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.RAIN)
			return false;
		
		var info = this.getCloudTypeAtWorldPos((float)pos.getX() + 0.5F, (float)pos.getZ() + 0.5F);
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
		var info = this.getCloudTypeAtWorldPos(x, z);
		CloudType type = info.getLeft();
		
		if (!type.weatherType().includesRain())
			return 0.0F;
		
		float fade = info.getRight();
		float verticalFade = 1.0F - Mth.clamp((y - (type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + this.getCloudHeight())) / SimpleCloudsConstants.RAIN_VERTICAL_FADE, 0.0F, 1.0F);
		return Math.min(1.0F, Math.max(0.0F, SimpleCloudsConstants.RAIN_THRESHOLD - fade) / SimpleCloudsConstants.RAIN_FADE) * verticalFade;
	}

	public void init(long seed)
	{
		RandomSource random = this.setSeed(seed);
		this.random = random;
		this.speed = 1.0F;
		this.cloudGenerator.initialize(random, this.level);
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

		if (this.isCloudGeneratorActive())
			this.cloudGenerator.tick(this.level);
		
		this.scrollXO = this.scrollX;
		this.scrollYO = this.scrollY;
		this.scrollZO = this.scrollZ;
		float speed = this.getSpeed();
		speed = this.modifySpeed(speed);
		speed *= 0.0001F;
		this.scrollAngle += speed;
		this.scrollX = (float)Math.cos(this.scrollAngle) * SCROLL_OFFSET;
		this.scrollY = 0.0F;//(float)Math.sin(this.scrollAngle + (float)Math.PI / 4.0F) * SCROLL_OFFSET * 0.5F;
		this.scrollZ = (float)Math.sin(this.scrollAngle) * SCROLL_OFFSET;
		
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
		var info = this.getCloudTypeAtWorldPos((float)x + 0.5F, (float)z + 0.5f);
		this.spawnLightning(info.getLeft(), info.getRight(), x, z, soundOnly);
	}
	
	public Vector2f calculateWindDirection()
	{
		float dirX = Mth.cos(this.scrollAngle);
		float dirZ = Mth.sin(this.scrollAngle);
		return new Vector2f(dirX, dirZ);
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
	
	protected float modifySpeed(float speed)
	{
		return speed;
	}
	
	public float getSpeed()
	{
		return this.speed;
	}
	
	public void setSpeed(float speed)
	{
		this.speed = Math.max(0.0F, speed);
	}
	
	public float getScrollAngle()
	{
		return this.scrollAngle;
	}
	
	public void setScrollAngle(float angle)
	{
		this.scrollAngle = angle;
	}

	public float getScrollX()
	{
		return this.scrollX;
	}

	public float getScrollY()
	{
		return this.scrollY;
	}

	public float getScrollZ()
	{
		return this.scrollZ;
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
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "[level=" + this.level.dimension().location() + "]";
	}
}
