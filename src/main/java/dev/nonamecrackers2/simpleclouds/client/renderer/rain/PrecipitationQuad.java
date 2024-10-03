package dev.nonamecrackers2.simpleclouds.client.renderer.rain;

import java.util.Map;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class PrecipitationQuad
{
	public static final float MAX_LENGTH = 32.0F;
	public static final float MAX_WIDTH = 2.0F;
	public static final Map<Biome.Precipitation, ResourceLocation> TEXTURE_BY_PRECIPITATION = Util.make(() -> {
		ImmutableMap.Builder<Biome.Precipitation, ResourceLocation> map = ImmutableMap.builder();
		map.put(Biome.Precipitation.RAIN, ResourceLocation.withDefaultNamespace("textures/environment/rain.png"));
		map.put(Biome.Precipitation.SNOW, ResourceLocation.withDefaultNamespace("textures/environment/snow.png"));
		return map.build();
	});
	private final Biome.Precipitation precipitation;
	private final BlockPos blockPos;
	private final Vector3f position;
	private final int lifeSpan;
	private final float initialWidth;
	private final Function<ClipContext, BlockHitResult> raycaster;
	private float length = MAX_LENGTH;
	private float xRot;
	private float yRot;
	private int tickCount;
	private float widthO;
	private float width;
	
	public PrecipitationQuad(Biome.Precipitation precipitation, Function<ClipContext, BlockHitResult> raycaster, BlockPos position, float xRot, float yRot, int lifeSpan, float initialWidth)
	{
		if (precipitation == Biome.Precipitation.NONE)
			throw new IllegalArgumentException("Cannot be NONE precipitation type");
		this.precipitation = precipitation;
		this.raycaster = raycaster;
		this.blockPos = position;
		this.position = new Vector3f(position.getX() + 0.5F, position.getY() + 0.5F, position.getZ() + 0.5F);
		this.xRot = xRot;
		this.yRot = yRot;
		this.lifeSpan = lifeSpan;
		this.initialWidth = Math.max(0.1F, initialWidth);// Mth.clamp(initialWidth, 0.1F, MAX_WIDTH);
	}
	
	public Biome.Precipitation getPrecipitation()
	{
		return this.precipitation;
	}
	
	public Vector3f getPos()
	{
		return this.position;
	}
	
	public BlockPos getBlockPos()
	{
		return this.blockPos;
	}
	
	public float getLength()
	{
		return this.length;
	}
	
	public float getXRot()
	{
		return this.xRot;
	}
	
	public void setXRot(float rot)
	{
		this.xRot = rot;
	}
	
	public float getYRot()
	{
		return this.yRot;
	}
	
	public void setYRot(float rot)
	{
		this.yRot = rot;
	}
	
	public int getTickCount()
	{
		return this.tickCount;
	}
	
	public boolean isDead()
	{
		return this.tickCount > this.lifeSpan;
	}
	
	public void tick()
	{
		this.tickCount++;
		
		Vec3 start = new Vec3(this.position);
		float yawRadians = -this.yRot;
		float pitchRadians = this.xRot - (float)Math.PI / 2.0F;
		float pitchCos = Mth.cos(pitchRadians);
		Vec3 end = new Vec3(Mth.sin(yawRadians) * pitchCos, Mth.sin(pitchRadians), Mth.cos(yawRadians) * pitchCos).scale(MAX_LENGTH).add(start);
		ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty());
		BlockHitResult result = this.raycaster.apply(context);
		Vec3 hit = result.getLocation();
		this.length = (float)start.distanceTo(hit);
		
		this.widthO = this.width;
		if (this.tickCount < this.lifeSpan - 20)
			this.width = this.initialWidth * Math.min(1.0F, (float)this.tickCount / 20.0F);
		else
			this.width = this.initialWidth * Math.min(1.0F, ((float)this.lifeSpan - (float)this.tickCount) / 20.0F);
	}
	
	public void render(PoseStack stack, VertexConsumer consumer, float partialTick, int packedLight, double camX, double camY, double camZ)
	{
		stack.translate(this.position.x, this.position.y, this.position.z);
		Quaternionf inverseRotation = new Quaternionf();
		inverseRotation.rotateX(this.xRot);
		inverseRotation.rotateY(this.yRot);
		Vector3f adjustedCamPos = new Vector3f((float)camX, (float)camY, (float)camZ).sub(this.position).rotate(inverseRotation).add(this.position);
		float angleToCam = (float)Mth.atan2(this.position.x - adjustedCamPos.x, this.position.z - adjustedCamPos.z);
		stack.mulPose(inverseRotation.invert());
		stack.mulPose(Axis.YP.rotation(angleToCam));
		Matrix4f mat = stack.last().pose();
		float vOffset = ((float)this.tickCount + partialTick) * (this.precipitation == Biome.Precipitation.RAIN ? -0.1F : -0.01F);
		float width = Mth.lerp(partialTick, this.widthO, this.width);
		float u1 = width / 2.0F * 0.5F + 0.5F;
		float u0 = 0.5F - width / 2.0F * 0.5F;
		consumer.addVertex(mat, width / 2.0F, 0.0F, 0.0F).setUv(u0, vOffset).setColor(1.0F, 1.0F, 1.0F, 1.0F).setLight(packedLight);
		consumer.addVertex(mat, -width / 2.0F, 0.0F, 0.0F).setUv(u1, vOffset).setColor(1.0F, 1.0F, 1.0F, 1.0F).setLight(packedLight);
		consumer.addVertex(mat, -width / 2.0F, -this.length, 0.0F).setUv(u1, this.length / 10.0F + vOffset).setColor(1.0F, 1.0F, 1.0F, 1.0F).setLight(packedLight);
		consumer.addVertex(mat, width / 2.0F, -this.length, 0.0F).setUv(u0, this.length / 10.0F + vOffset).setColor(1.0F, 1.0F, 1.0F, 1.0F).setLight(packedLight);
	}
}
