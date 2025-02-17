package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2f;

import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class CloudRegion
{
	private final ResourceLocation cloudTypeId;
	private final float initialRadius;
	private final Vec2 movementDirection;
	private final float maxSpeed;
	private final float accelerationFactor;
	private float velX;
	private float velZ;
	private float posX;
	private float posXO;
	private float posZ;
	private float posZO;
	private float radius;
	private float radiusO;
	private int tickCount;
	private int existsForTicks;
	
	public CloudRegion(ResourceLocation cloudTypeId, Vec2 movementDirection, float maxSpeed, float accelerationFactor, float posX, float posY, float radius, int existsForTicks)
	{
		this.cloudTypeId = cloudTypeId;
		this.movementDirection = movementDirection;
		this.maxSpeed = maxSpeed;
		this.accelerationFactor = accelerationFactor;
		this.posX = posX;
		this.posZ = posY;
		this.initialRadius = radius;
		this.radius = radius;
		this.existsForTicks = existsForTicks;
	}
	
	public CloudRegion(FriendlyByteBuf buffer)
	{
		this.cloudTypeId = buffer.readResourceLocation();
		this.initialRadius = buffer.readFloat();
		this.movementDirection = new Vec2(buffer.readFloat(), buffer.readFloat());
		this.maxSpeed = buffer.readFloat();
		this.accelerationFactor = buffer.readFloat();
		this.velX = buffer.readFloat();
		this.velZ = buffer.readFloat();
		this.posX = buffer.readFloat();
		this.posXO = this.posX;
		this.posZ = buffer.readFloat();
		this.posZO = this.posZ;
		this.radius = buffer.readFloat();
		this.radiusO = this.radius;
		this.tickCount = buffer.readVarInt();
		this.existsForTicks = buffer.readVarInt();
	}
	
	public void toPacket(FriendlyByteBuf buffer)
	{
		buffer.writeResourceLocation(this.cloudTypeId);
		buffer.writeFloat(this.initialRadius);
		buffer.writeFloat(this.movementDirection.x);
		buffer.writeFloat(this.movementDirection.y);
		buffer.writeFloat(this.maxSpeed);
		buffer.writeFloat(this.accelerationFactor);
		buffer.writeFloat(this.velX);
		buffer.writeFloat(this.velZ);
		buffer.writeFloat(this.posX);
		buffer.writeFloat(this.posZ);
		buffer.writeFloat(this.radius);
		buffer.writeVarInt(this.tickCount);
		buffer.writeVarInt(this.existsForTicks);
	}

	public void tick(RandomSource random, Level level)
	{
		this.radiusO = this.radius;
		float scale = 1.0F - (float)this.tickCount / (float)this.existsForTicks;
		this.radius = this.initialRadius * scale;
		
		this.tickCount++;
		
		float targetVelX = Math.abs(this.movementDirection.x * this.maxSpeed);
		float targetVelZ = Math.abs(this.movementDirection.y * this.maxSpeed);
		this.velX = Mth.clamp(this.velX + this.movementDirection.x * this.accelerationFactor, -targetVelX, targetVelX);
		this.velZ = Mth.clamp(this.velZ + this.movementDirection.y * this.accelerationFactor, -targetVelZ, targetVelZ);
		
		this.posXO = this.posX;
		this.posZO = this.posZ;
		this.posX += this.velX;
		this.posZ += this.velZ;
	}
	
	public ResourceLocation getCloudTypeId()
	{
		return this.cloudTypeId;
	}
	
	public boolean isDead()
	{
		return this.tickCount > this.existsForTicks;
	}
	
	public Vec2 getMovementDirection()
	{
		return this.movementDirection;
	}

	public float getMaxSpeed()
	{
		return this.maxSpeed;
	}

	public float getAccelerationFactor()
	{
		return this.accelerationFactor;
	}
	
	public float getPosX(float partialTick)
	{
		return Mth.lerp(partialTick, this.posXO, this.posX);
	}
	
	public float getPosZ(float partialTick)
	{
		return Mth.lerp(partialTick, this.posZO, this.posZ);
	}
	
	public float getRadius(float partialTick)
	{
		return Mth.lerp(partialTick, this.radiusO, this.radius);
	}

	public float getPosX()
	{
		return this.posX;
	}

	public float getPosZ()
	{
		return this.posZ;
	}

	public float getRadius()
	{
		return this.radius;
	}
	
	private static CompositeResult circle(CloudRegion region, float x, float z)
	{
		float d = Vector2f.distance(region.posX, region.posZ, x, z);
		float eff = SimpleCloudsConstants.REGION_EDGE_FADE_FACTOR;
		if (d > region.radius + 1.0F / eff)
			return new CompositeResult(-1.0F, -1.0F, null);
		else if (d < region.radius)
			return new CompositeResult(Math.min((region.radius - d) * eff, 1.0F), 0.0F, region);
		else
			return new CompositeResult(0.0F, Math.min((d - region.radius) * eff, 1.0F), region);
	}
	
	private static void composite(Result old, CompositeResult toComposite)
	{
		if (toComposite.innerFactor > 0.0F)
		{
			if (old.region != null && old.region.cloudTypeId.equals(toComposite.regionAt.cloudTypeId))
			{
				old.fade = Mth.lerp(toComposite.innerFactor, old.fade, 1.0F);
			}
			else
			{
				old.region = toComposite.regionAt;
				old.fade = toComposite.innerFactor;
			}
		}
		else if (toComposite.outerFactor >= 0.0F)
		{
			if (old.region == null || !old.region.cloudTypeId.equals(toComposite.regionAt.cloudTypeId))
				old.fade *= toComposite.outerFactor;
		}
	}
	
	public static Pair<CloudRegion, Float> calculateAt(List<CloudRegion> regions, float x, float z)
	{
		Result result = new Result();
		for (CloudRegion region : regions)
			composite(result, circle(region, x, z));
		return Pair.of(result.region, result.fade);
	}
	
	private static record CompositeResult(float innerFactor, float outerFactor, CloudRegion regionAt) {}
	
	private static class Result
	{
		private @Nullable CloudRegion region;
		private float fade;
	}
}
