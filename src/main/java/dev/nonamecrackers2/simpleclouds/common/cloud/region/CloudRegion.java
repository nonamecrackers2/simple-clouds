package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix2f;
import org.joml.Matrix3f;
import org.joml.Vector2f;

import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
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
	private final int orderWeight;
	private float velX;
	private float velZ;
	private float posX;
	private float posXO;
	private float posZ;
	private float posZO;
	private float radius;
	private float radiusO;
	private float stretchFactor;
	private float stretchFactorO;
	private float rotation;
	private float rotationO;
	private int tickCount;
	private int existsForTicks;
	private int growTicks;
	private boolean priorVisible;
	
	public CloudRegion(ResourceLocation cloudTypeId, Vec2 movementDirection, float maxSpeed, float accelerationFactor, float posX, float posZ, float radius, float rotation, float stretchFactor, int existsForTicks, int growTicks, int orderWeight)
	{
		this.cloudTypeId = cloudTypeId;
		this.movementDirection = movementDirection;
		this.maxSpeed = maxSpeed;
		this.accelerationFactor = accelerationFactor;
		this.posX = posX;
		this.posZ = posZ;
		this.initialRadius = radius;
		this.radius = 0;
		this.rotation = rotation;
		this.stretchFactor = Math.max(0.01F, stretchFactor);
		this.existsForTicks = Math.max(0, existsForTicks);
		this.growTicks = Mth.clamp(growTicks, 0, existsForTicks);
		this.orderWeight = orderWeight;
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
		this.stretchFactor = buffer.readFloat();
		this.stretchFactorO = this.stretchFactor;
		this.rotation = buffer.readFloat();
		this.rotationO = this.rotation;
		this.tickCount = buffer.readVarInt();
		this.existsForTicks = buffer.readVarInt();
		this.growTicks = buffer.readVarInt();
		this.orderWeight = buffer.readVarInt();
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
		buffer.writeFloat(this.stretchFactor);
		buffer.writeFloat(this.rotation);
		buffer.writeVarInt(this.tickCount);
		buffer.writeVarInt(this.existsForTicks);
		buffer.writeVarInt(this.growTicks);
		buffer.writeVarInt(this.orderWeight);
	}

	public void tick(RandomSource random, Level level, boolean isVisible)
	{
		this.radiusO = this.radius;
		this.stretchFactorO = this.stretchFactor;
		this.rotationO = this.rotation;
		float scale;
		if (this.tickCount < this.growTicks)
			scale = (float)this.tickCount / (float)this.growTicks;
		else
			scale = 1.0F - (float)(this.tickCount - this.growTicks) / (float)this.existsForTicks;
		this.radius = this.initialRadius * scale;
		
		this.tickCount += isVisible ? 1 : 20; //TODO: Test this
		
		this.posXO = this.posX;
		this.posZO = this.posZ;

		if (isVisible)
		{
			float targetVelX = Math.abs(this.movementDirection.x * this.maxSpeed);
			float targetVelZ = Math.abs(this.movementDirection.y * this.maxSpeed);
			this.velX = Mth.clamp(this.velX + this.movementDirection.x * this.accelerationFactor, -targetVelX, targetVelX);
			this.velZ = Mth.clamp(this.velZ + this.movementDirection.y * this.accelerationFactor, -targetVelZ, targetVelZ);
			this.posX += this.velX;
			this.posZ += this.velZ;
		}
		
		this.priorVisible = isVisible;
	}
	
	public ResourceLocation getCloudTypeId()
	{
		return this.cloudTypeId;
	}
	
	public int getOrderWeight()
	{
		return this.orderWeight;
	}
	
	public boolean intersects(SpawnRegion region)
	{
		return region.intersectsCircle(this.getWorldX(), this.getWorldZ(), this.getWorldRadius() + (float)SimpleCloudsConstants.CLOUD_SCALE / SimpleCloudsConstants.REGION_EDGE_FADE_FACTOR);
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
	
	public float getStretch(float partialTick)
	{
		return Mth.lerp(partialTick, this.stretchFactorO, this.stretchFactor);
	}
	
	public float getRotation(float partialTick)
	{
		return Mth.lerp(partialTick, this.rotationO, this.rotation);
	}

	public float getPosX()
	{
		return this.posX;
	}
	
	public float getWorldX()
	{
		return this.posX * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}

	public float getPosZ()
	{
		return this.posZ;
	}
	
	public float getWorldZ()
	{
		return this.posZ * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}

	public float getRadius()
	{
		return this.radius;
	}
	
	public float getWorldRadius()
	{
		return this.radius * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}
	
	public float getStretch()
	{
		return this.stretchFactor;
	}
	
	public float getRotation()
	{
		return this.rotation;
	}
	
	public boolean wasPriorVisible()
	{
		return this.priorVisible;
	}
	
	public Matrix2f createTransform(float partialTick)
	{
		Matrix2f transform = new Matrix2f().identity();
		transform.scale(this.getStretch(partialTick), 1.0F);
		transform.rotate(this.getRotation(partialTick));
		return transform;
	}
	
	private static CompositeResult circle(CloudRegion region, float x, float z)
	{
		Matrix2f transform = region.createTransform(1.0F);
		Vector2f pos = new Vector2f(x, z).sub(region.posX, region.posZ).mul(transform).add(region.posX, region.posZ);
		float d = pos.distance(region.posX, region.posZ);
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
