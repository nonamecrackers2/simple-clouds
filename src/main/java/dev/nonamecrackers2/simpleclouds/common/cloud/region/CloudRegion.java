package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix2f;
import org.joml.Vector2f;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.region.ScAPICloudRegion;
import dev.nonamecrackers2.simpleclouds.api.common.event.CloudRegionTickEvent;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.common.MinecraftForge;
import nonamecrackers2.crackerslib.common.util.primitives.PrimitiveHelper;

public class CloudRegion implements ScAPICloudRegion
{
	private final ResourceLocation cloudTypeId;
	private final float initialRadius;
	private final int orderWeight;
	private Vec2 movementDirection;
	private float maxSpeed;
	private float accelerationFactor;
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
	
	public CloudRegion(CompoundTag tag) throws IllegalArgumentException
	{
		this.cloudTypeId = ResourceLocation.read(tag.getString("id")).resultOrPartial(e -> {
			throw new IllegalArgumentException(e);
		}).get();
		this.initialRadius = tag.getFloat("initial_radius");
		this.movementDirection = PrimitiveHelper.vec2FromTag(tag.getCompound("movement_direction"));
		this.maxSpeed = tag.getFloat("max_speed");
		this.accelerationFactor = tag.getFloat("acceleration_factor");
		this.orderWeight = tag.getInt("order_weight");
		CompoundTag vel = tag.getCompound("velocity");
		this.velX = vel.getFloat("x");
		this.velZ = vel.getFloat("z");
		CompoundTag pos = tag.getCompound("pos");
		this.posX = pos.getFloat("x");
		this.posXO = this.posX;
		this.posZ = pos.getFloat("z");
		this.posZO = this.posZ;
		this.radius = tag.getFloat("radius");
		this.radiusO = this.radius;
		this.stretchFactor = tag.getFloat("stretch_factor");
		this.stretchFactorO = this.stretchFactor;
		this.rotation = tag.getFloat("rotation");
		this.rotationO = this.rotation;
		this.tickCount = tag.getInt("tick_count");
		this.existsForTicks = tag.getInt("exists_for_ticks");
		this.growTicks = tag.getInt("grow_ticks");
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
	
	public CompoundTag toTag()
	{
		CompoundTag tag = new CompoundTag();
		tag.putString("id", this.cloudTypeId.toString());
		tag.putFloat("initial_radius", this.initialRadius);
		tag.put("movement_direction", PrimitiveHelper.vec2ToTag(this.movementDirection));
		tag.putFloat("max_speed", this.maxSpeed);
		tag.putFloat("acceleration_factor", this.accelerationFactor);
		tag.putInt("order_weight", this.orderWeight);
		CompoundTag vel = new CompoundTag();
		vel.putFloat("x", this.velX);
		vel.putFloat("z", this.velZ);
		tag.put("velocity", vel);
		CompoundTag pos = new CompoundTag();
		pos.putFloat("x", this.posX);
		pos.putFloat("z", this.posZ);
		tag.put("pos", pos);
		tag.putFloat("radius", this.radius);
		tag.putFloat("stretch_factor", this.stretchFactor);
		tag.putFloat("rotation", this.rotation);
		tag.putInt("tick_count", this.tickCount);
		tag.putInt("exists_for_ticks", this.existsForTicks);
		tag.putInt("grow_ticks", this.growTicks);
		return tag;
	}

	public void tick(RandomSource random, Level level, boolean isVisible)
	{
		CloudRegionTickEvent event = new CloudRegionTickEvent(level, this);
		MinecraftForge.EVENT_BUS.post(event);
		Vec2 movementDirection = this.movementDirection;
		float maxSpeed = this.maxSpeed;
		float accelerationFactor = this.accelerationFactor;
		if (event.getModifiedMovementDirection() != null)
			movementDirection = event.getModifiedMovementDirection();
		if (event.getModifiedMaxSpeed() >= 0.0F)
			maxSpeed = event.getModifiedMaxSpeed();
		if (event.getModifiedAccelerationFactor() >= 0.0F)
			accelerationFactor = event.getModifiedAccelerationFactor();
		
		this.radiusO = this.radius;
		this.stretchFactorO = this.stretchFactor;
		this.rotationO = this.rotation;
		float scale;
		if (this.tickCount < this.growTicks)
			scale = (float)this.tickCount / (float)this.growTicks;
		else
			scale = 1.0F - (float)(this.tickCount - this.growTicks) / (float)(this.existsForTicks - this.growTicks);
		this.radius = this.initialRadius * scale;
		
		this.tickCount += isVisible ? 1 : 20;
		
		this.posXO = this.posX;
		this.posZO = this.posZ;

		if (isVisible)
		{
			float targetVelX = Math.abs(movementDirection.x * maxSpeed);
			float targetVelZ = Math.abs(movementDirection.y * maxSpeed);
			this.velX = Mth.clamp(this.velX + movementDirection.x * accelerationFactor, -targetVelX, targetVelX);
			this.velZ = Mth.clamp(this.velZ + movementDirection.y * accelerationFactor, -targetVelZ, targetVelZ);
			this.posX += this.velX;
			this.posZ += this.velZ;
		}
		
		this.priorVisible = isVisible;
	}
	
	@Override
	public ResourceLocation getCloudTypeId()
	{
		return this.cloudTypeId;
	}
	
	@Override
	public int getOrderWeight()
	{
		return this.orderWeight;
	}
	
	public boolean intersects(SpawnRegion region)
	{
		return region.intersectsCircle(this.getWorldX(), this.getWorldZ(), this.getWorldRadius() + (float)SimpleCloudsConstants.CLOUD_SCALE / SimpleCloudsConstants.REGION_EDGE_FADE_FACTOR);
	}
	
	@Override
	public boolean isDead()
	{
		return this.tickCount > this.existsForTicks;
	}
	
	@Override
	public Vec2 getMovementDirection()
	{
		return this.movementDirection;
	}
	
	@Override
	public void setMovementDirection(Vec2 direction)
	{
		this.movementDirection = direction;
	}
	
	@Override
	public float getMaxSpeed()
	{
		return this.maxSpeed;
	}
	
	@Override
	public void setMaxSpeed(float speed)
	{
		this.maxSpeed = speed;
	}
	
	@Override
	public float getAccelerationFactor()
	{
		return this.accelerationFactor;
	}
	
	@Override
	public void setAccelerationFactor(float factor)
	{
		this.accelerationFactor = factor;
	}
	
	@Override
	public float getPosX(float partialTick)
	{
		return Mth.lerp(partialTick, this.posXO, this.posX);
	}
	
	@Override
	public float getPosX()
	{
		return this.posX;
	}
	
	@Override
	public float getWorldX()
	{
		return this.posX * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}
	
	@Override
	public float getPosZ(float partialTick)
	{
		return Mth.lerp(partialTick, this.posZO, this.posZ);
	}
	
	@Override
	public float getPosZ()
	{
		return this.posZ;
	}
	
	@Override
	public float getWorldZ()
	{
		return this.posZ * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}
	
	@Override
	public void moveTo(float x, float z)
	{
		this.posX = x;
		this.posXO = x;
		this.posZ = z;
		this.posZO = z;
	}
	
	@Override
	public void moveToWorldPos(float x, float z)
	{
		this.moveTo(x / (float)SimpleCloudsConstants.CLOUD_SCALE, z / (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	@Override
	public float getRadius(float partialTick)
	{
		return Mth.lerp(partialTick, this.radiusO, this.radius);
	}
	
	@Override
	public float getRadius()
	{
		return this.radius;
	}
	
	@Override
	public float getWorldRadius()
	{
		return this.radius * (float)SimpleCloudsConstants.CLOUD_SCALE;
	}
	
	@Override
	public void setRadius(float radius)
	{
		this.radius = radius;
		this.radiusO = radius;
	}
	
	@Override
	public void setWorldRadius(float radius)
	{
		this.setRadius(radius / (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	@Override
	public float getStretch(float partialTick)
	{
		return Mth.lerp(partialTick, this.stretchFactorO, this.stretchFactor);
	}
	
	@Override
	public float getStretch()
	{
		return this.stretchFactor;
	}
	
	@Override
	public void setStretchFactor(float factor)
	{
		this.stretchFactor = factor;
		this.stretchFactorO = factor;
	}
	
	@Override
	public float getRotation(float partialTick)
	{
		return Mth.lerp(partialTick, this.rotationO, this.rotation);
	}
	
	@Override
	public float getRotation()
	{
		return this.rotation;
	}
	
	@Override
	public void setRotation(float rotation)
	{
		this.rotation = rotation;
		this.rotationO = rotation;
	}
	
	@Override
	public boolean wasPriorVisible()
	{
		return this.priorVisible;
	}
	
	@Override
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
