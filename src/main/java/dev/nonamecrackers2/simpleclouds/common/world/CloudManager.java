package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import net.minecraft.resources.ResourceLocation;
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
	private long seed;
	private float scrollXO;
	private float scrollYO;
	private float scrollZO;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private Vector3f direction = new Vector3f(1.0F, 0.0F, 0.0F);
	private float speed = 1.0F;
//	private CloudMode cloudMode = CloudMode.DEFAULT;
//	private float singleModeFadeStart = 0.8F;
//	private float singleModeFadeEnd = 1.0F;
//	private ResourceLocation singleModeCloudType = SimpleCloudsMod.id("itty_bitty");
	private int cloudHeight = 128;
	private int tickCount;
	private boolean requiresSync;

	public static CloudManager get(Level level)
	{
		return Objects.requireNonNull(((CloudManagerAccessor)level).getCloudManager(), "Cloud manager is not available, this shouldn't happen!");
	}
	
	public CloudManager(Level level)
	{
		this.level = level;
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
//	
//	public void setCloudMode(CloudMode mode)
//	{
//		this.cloudMode = mode;
//	}
//	
//	public CloudMode getCloudMode()
//	{
//		return this.cloudMode;
//	}
//	
//	public void setSingleModeCloudType(ResourceLocation cloudType)
//	{
//		this.singleModeCloudType = cloudType;
//	}
//	
//	public ResourceLocation getSingleModeCloudType()
//	{
//		return this.singleModeCloudType;
//	}
//	
//	public float getSingleModeFadeStart()
//	{
//		return this.singleModeFadeStart;
//	}
//
//	public void setSingleModeFadeStart(float singleModeFadeStart)
//	{
//		this.singleModeFadeStart = Mth.clamp(singleModeFadeStart, 0.0F, 1.0F);
//	}
//
//	public float getSingleModeFadeEnd()
//	{
//		return this.singleModeFadeEnd;
//	}
//
//	public void setSingleModeFadeEnd(float singleModeFadeEnd)
//	{
//		this.singleModeFadeEnd = Mth.clamp(singleModeFadeEnd, 0.0F, 1.0F);
//	}
	
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
	
	public void requiresSync()
	{
		this.requiresSync = true;
	}
	
	public boolean checkAndResetNeedsSync()
	{
		if (this.requiresSync)
		{
			this.requiresSync = false;
			return true;
		}
		else
		{
			return false;
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
}
