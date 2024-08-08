package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.init.RegionTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class CloudManager
{
	public static final int CLOUD_HEIGHT_MAX = 2048;
	public static final int CLOUD_HEIGHT_MIN = 0;
	public static final int UPDATE_INTERVAL = 200;
	public static final float RANDOM_SPREAD = 10000.0F;
	private final Level level;
	private RegionType regionGenerator = RegionTypes.VORONOI_DIAGRAM.get();
	private long seed;
	private float scrollXO;
	private float scrollYO;
	private float scrollZO;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private Vector3f direction = new Vector3f(1.0F, 0.0F, 0.0F);
	private float speed = 1.0F;
	private int cloudHeight = 128;
	private int tickCount;
	private CloudManager.SyncType syncType = CloudManager.SyncType.NONE;

	public static CloudManager get(Level level)
	{
		return Objects.requireNonNull(((CloudManagerAccessor)level).getCloudManager(), "Cloud manager is not available, this shouldn't happen!");
	}
	
	public CloudManager(Level level)
	{
		this.level = level;
	}
	
	public CloudType[] getIndexedCloudTypes()
	{
		return CloudTypeDataManager.getServerInstance().getIndexedCloudTypes();
	}
	
	public Pair<CloudType, Float> getCloudTypeAtPosition(float x, float z)
	{
		CloudType[] types = this.getIndexedCloudTypes();
		float posX = this.scrollX + x / (float)CloudConstants.CLOUD_SCALE;
		float posZ = this.scrollZ + z / (float)CloudConstants.CLOUD_SCALE;
		var result = this.getRegionGenerator().getCloudTypeIndexAt(posX, posZ, CloudConstants.REGION_SCALE, types.length);
		if (result.index() < 0 || result.index() >= types.length)
			throw new IndexOutOfBoundsException("Region type generator received an invalid index: " + result.index());
		return Pair.of(types[result.index()], result.fade());
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
	}
	
	public void setRequiresSync(CloudManager.SyncType syncType)
	{
		this.syncType = syncType;
	}
	
	public CloudManager.SyncType getAndResetSync()
	{
		if (this.syncType != CloudManager.SyncType.NONE)
		{
			CloudManager.SyncType syncType = this.syncType;
			this.syncType = CloudManager.SyncType.NONE;
			return syncType;
		}
		else
		{
			return CloudManager.SyncType.NONE;
		}
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
	
	public static enum SyncType
	{
		BASE_PROPERTIES,
		MOVEMENT,
		NONE;
	}
}
