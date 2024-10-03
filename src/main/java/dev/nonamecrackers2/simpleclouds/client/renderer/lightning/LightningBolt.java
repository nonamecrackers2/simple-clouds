package dev.nonamecrackers2.simpleclouds.client.renderer.lightning;

import java.util.List;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class LightningBolt
{
	private static final int TOTAL_TIME = 60;
	private static final int STAY_DURATION = 10;
	private static final int FADE_IN_TIME = 2;
	private static final int FADE_OUT_TIME = 20;
	private static final float FLASH_INTENSITY = 2.0F;
	private static final float RESTRUCTURE_FADE_LIMIT = 0.1F;
	private static final int BRANCH_SEQUENCE_FADE_DURATION = 5;
	public static final int MAX_DEPTH = 16;
	public static final int MAX_BRANCHES = 8;
	public static final float MINIMUM_PITCH_ALLOWED = 0.0F;
	public static final float MAXIMUM_PITCH_ALLOWED = 180.0F;
	private final RandomSource random;
	private final int totalDepth;
	private final int branchCount;
	private final float maxBranchLength;
	private final float maxWidth;
	private final float maxPitch;
	private final float minPitch;
	private final Vector3f position;
	private List<LightningBolt.Branch> root;
	private int tickCount;
	private float fade;
	private float fadeO;
	private float r;
	private float g;
	private float b;
	
	public LightningBolt(RandomSource random, Vector3f position, int depth, int branchCount, float maxBranchLength, float maxWidth, float minimumPitch, float maximumPitch, float r, float g, float b)
	{
		this.random = random;
		this.totalDepth = Mth.clamp(depth, 1, MAX_DEPTH);
		this.branchCount = Mth.clamp(branchCount, 1, MAX_BRANCHES);
		this.maxBranchLength = maxBranchLength;
		this.maxWidth = maxWidth;
		this.minPitch = Mth.clamp(minimumPitch, MINIMUM_PITCH_ALLOWED, MAXIMUM_PITCH_ALLOWED);
		this.maxPitch = Mth.clamp(maximumPitch, minimumPitch, MAXIMUM_PITCH_ALLOWED);
		this.position = position;
		this.root = buildBranchesWithChildren(random, depth, 0, branchCount, maxBranchLength, maxWidth, maxWidth, maximumPitch, minimumPitch);
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	private static float calculateWidthAtDepth(int maxDepth, int desiredDepth, float maxWidth)
	{
		float width = maxWidth;
		for (int i = 0; i < desiredDepth; i++)
		{
			if (width <= 0.5F)
				return 0.5F;
			width = width - maxWidth / (float)(maxDepth + 1);
		}
		return width;
	}
	
	private static List<LightningBolt.Branch> buildBranchesWithChildren(RandomSource random, int totalDepth, int currentDepth, int branchCount, float maxBranchLength, float maxWidth, float width, float minPitch, float maxPitch)
	{
		if (currentDepth >= totalDepth)
			return Lists.newArrayList();
		List<LightningBolt.Branch> branches = Lists.newArrayList();
		for (int i = 0; i < branchCount; i++)
		{
			float pitch = (maxPitch - minPitch) * random.nextFloat() + minPitch;
			float yaw = 360.0F * random.nextFloat();
			float length = maxBranchLength / 4.0F + maxBranchLength * random.nextFloat();
			float nextWidth = Math.max(0.5F, width - maxWidth / (float)(totalDepth + 1));
			int range = Mth.floor((float)totalDepth - 1.0F / (float)totalDepth * ((float)currentDepth * (float)currentDepth));
			int nextBranchCount = range <= 0 ? 0 : Math.min(random.nextInt(range), branchCount);
			if ((float)currentDepth / (float)totalDepth < 0.5F)
				nextBranchCount = Math.max(nextBranchCount, 1);
			List<LightningBolt.Branch> children = buildBranchesWithChildren(random, totalDepth, currentDepth + 1, nextBranchCount, maxBranchLength, maxWidth, nextWidth, minPitch, maxPitch);
			LightningBolt.Branch branch = new LightningBolt.Branch(children, pitch, yaw, width, length);
			branches.add(branch);
		}
		return branches;
	}
	
	private static @Nullable List<LightningBolt.Branch> getBranchesAtDepth(List<LightningBolt.Branch> root, int atDepth, int currentDepth)
	{
		if (currentDepth == atDepth)
			return root;
		for (LightningBolt.Branch branch : root)
		{
			var list = getBranchesAtDepth(branch.branches, atDepth, currentDepth + 1);
			if (list != null)
				return list;
		}
		return null;
	}
	
	private List<LightningBolt.Branch> getBranchesAtDepth(int depth)
	{
		var branches = getBranchesAtDepth(this.root, depth, 0);
		if (branches == null)
			return Lists.newArrayList();
		else
			return branches;
	}
	
	public void tick()
	{
		this.tickCount++;
		
		if (this.tickCount < TOTAL_TIME)
		{
			this.fadeO = this.fade;
			this.fade = 1.0F;
			if (this.tickCount < STAY_DURATION)
				this.fade = Math.min(1.0F, (float)this.tickCount / (float)FADE_IN_TIME);
			else
				this.fade = Math.max(0.0F, 1.0F - (float)(this.tickCount - STAY_DURATION) / (float)FADE_OUT_TIME);
			
			this.fade = this.fade * (float)Math.pow((double)this.random.nextFloat(), (double)FLASH_INTENSITY);
			
			if (this.fade > RESTRUCTURE_FADE_LIMIT)
			{
				int maxDepth = this.totalDepth - Mth.floor((float)this.totalDepth * ((float)this.tickCount / (float)TOTAL_TIME));
				int depth = this.totalDepth - (maxDepth <= 1 ? 0 : this.random.nextInt(maxDepth));
				float width = calculateWidthAtDepth(this.totalDepth, depth, this.maxWidth);
				List<LightningBolt.Branch> branches = this.getBranchesAtDepth(depth);
				if (!branches.isEmpty())
				{
					int index = branches.size() <= 1 ? 0 : this.random.nextInt(branches.size());
					LightningBolt.Branch branch = branches.get(index);
					branch.setBranches(buildBranchesWithChildren(this.random, this.totalDepth - depth, 0, this.branchCount, this.maxBranchLength, this.maxWidth, width, this.minPitch, this.maxPitch));
				}
			}
		}
	}
	
	public boolean isDead()
	{
		return this.tickCount > TOTAL_TIME;
	}
	
	public void render(PoseStack stack, VertexConsumer consumer, float partialTick, float r, float g, float b, float a)
	{
		float alpha = Mth.lerp(partialTick, this.fadeO, this.fade) * a;
		if (alpha <= 0.01F)
			return;
		
		stack.pushPose();
		stack.translate(this.position.x, this.position.y, this.position.z);
		
		float animFactor = ((float)this.tickCount + partialTick) / (float)BRANCH_SEQUENCE_FADE_DURATION;
		int depth = Mth.floor((float)this.totalDepth * animFactor);
		
		for (LightningBolt.Branch branch : this.root)
			renderBranch(depth, 0, new Vector3f(), stack, consumer, r * this.r, g * this.g, b * this.b, alpha, branch);
		
		stack.popPose();
	}
	
	public Vector3f getPosition()
	{
		return this.position;
	}
	
	public float getFade(float partialTick)
	{
		return Mth.lerp(partialTick, this.fadeO, this.fade);
	}
	
	private static void renderBranch(int maxDepth, int currentDepth, Vector3f offset, PoseStack stack, VertexConsumer consumer, float r, float g, float b, float a, LightningBolt.Branch branch)
	{
		if (currentDepth > maxDepth)
			return;
		stack.pushPose();
		stack.translate(offset.x, offset.y, offset.z);
		stack.mulPose(Axis.YP.rotationDegrees(branch.yaw));
		stack.mulPose(Axis.XP.rotationDegrees(branch.pitch));
		Matrix4f mat = stack.last().pose();
		int layers = 4;
		for (int i = 0; i < layers; i++)
		{
			float factor = (float)i / (float)layers;
			float width = branch.width - 4.0F * (branch.width / 4.0F) * factor;
			if (width <= 0.05F)
				continue;
			float length = branch.length - factor;
			float startingY = -factor * 0.5F;
			float alpha = (float)(i + 1) / (float)layers * 0.5F;
			lightningBoltSection(mat, consumer, startingY, width, length, r, g, b, alpha * a);
		}
		float yawRadians = branch.yaw * ((float)Math.PI / 180.0F);
		float pitchRadians = (90.0F - branch.pitch) * ((float)Math.PI / 180.0F);
		float pitchCos = Mth.cos(pitchRadians);
		Vector3f end = new Vector3f(Mth.sin(yawRadians) * pitchCos, Mth.sin(pitchRadians), Mth.cos(yawRadians) * pitchCos).mul(-branch.length).add(offset);
		stack.popPose();
		for (LightningBolt.Branch child : branch.branches)
			renderBranch(maxDepth, currentDepth + 1, end, stack, consumer, r, g, b, a, child);
	}
	
	private static void lightningBoltSection(Matrix4f poseMatrix, VertexConsumer consumer, float yStart, float width, float length, float r, float g, float b, float a)
	{
		float halfWidth = width / 2.0F;
		consumer.addVertex(poseMatrix, +halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, +halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		
		consumer.addVertex(poseMatrix, +halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, +halfWidth, yStart, halfWidth).setColor(r, g, b, a);
		
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart, halfWidth).setColor(r, g, b, a);
		
		consumer.addVertex(poseMatrix, halfWidth, yStart, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart - length, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart - length, -halfWidth).setColor(r, g, b, a);
		
		consumer.addVertex(poseMatrix, -halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart, -halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, halfWidth, yStart, halfWidth).setColor(r, g, b, a);
		consumer.addVertex(poseMatrix, -halfWidth, yStart, halfWidth).setColor(r, g, b, a);
	}
	
	public static class Branch
	{
		private List<LightningBolt.Branch> branches;
		private final float pitch;
		private final float yaw;
		private final float width;
		private final float length;
		
		public Branch(List<LightningBolt.Branch> branches, float pitch, float yaw, float width, float length)
		{
			this.branches = branches;
			this.pitch = pitch;
			this.yaw = yaw;
			this.width = width;
			this.length = length;
		}
		
		private void setBranches(List<LightningBolt.Branch> branches)
		{
			this.branches = branches;
		}
	}
}
