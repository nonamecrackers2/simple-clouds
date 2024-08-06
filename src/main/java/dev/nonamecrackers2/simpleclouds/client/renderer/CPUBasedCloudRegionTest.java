package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Math;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.platform.TextureUtil;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import net.minecraft.util.Mth;

@Deprecated(forRemoval = true)
public class CPUBasedCloudRegionTest
{
	private @Nullable ByteBuffer textureBuffer;
	private int textureId = -1;
	private int texSize;
	private float scale;
	private float scrollX;
	private float scrollZ;
	private float cameraOffsetX;
	private float cameraOffsetZ;
	private float cloudScale;
	private CloudType[] types;
	
	public void init(CloudType[] types, int texSize, float scale, float scrollX, float scrollZ, double camX, double camZ, float cloudScale)
	{
		this.texSize = texSize;
		this.scale = scale;
		this.scrollX = scrollX;
		this.scrollZ = scrollZ;
		float chunkSizeUpscaled = 32.0F * cloudScale;
		float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * 32.0F);
		float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * 32.0F);
		this.cameraOffsetX = camOffsetX;
		this.cameraOffsetZ = camOffsetZ;
		this.cloudScale = cloudScale;
		this.types = Arrays.copyOf(types, types.length);
		
		if (this.textureBuffer != null)
		{
			MemoryUtil.memFree(this.textureBuffer);
			this.textureBuffer = null;
		}
		
		int size = this.texSize * this.texSize * 4 * 4;
		this.textureBuffer = MemoryTracker.create(size);
		
		this.generateTexture();
		
		if (this.textureId != -1)
		{
			TextureUtil.releaseTextureId(this.textureId);
			this.textureId = -1;
		}
		
		this.textureId = TextureUtil.generateTextureId();
		GL11.glBindTexture(GL12.GL_TEXTURE_2D, this.textureId);
		GL11.glTexParameteri(GL12.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL12.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexImage2D(GL12.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, this.texSize, this.texSize, 0, GL11.GL_RGBA, GL11.GL_FLOAT, this.textureBuffer);
		GL11.glBindTexture(GL12.GL_TEXTURE_2D, 0);
	}
	
	public void regenerate()
	{
		this.generateTexture();
		GL11.glBindTexture(GL12.GL_TEXTURE_2D, this.textureId);
		GL11.glTexSubImage2D(GL12.GL_TEXTURE_2D, 0, 0, 0, this.texSize, this.texSize, GL11.GL_RGBA, GL11.GL_FLOAT, this.textureBuffer);
		GL11.glBindTexture(GL12.GL_TEXTURE_2D, 0);
	}
	
	private void generateTexture()
	{
		for (int x = 0; x < this.texSize; x++)
		{
			for (int y = 0; y < this.texSize; y++)
			{
				int index = (x + y * this.texSize) * 4 * 4;
				Vector2d uv = new Vector2d((float)x, (float)y).sub((float)this.texSize / 2.0F, (float)this.texSize / 2.0F).mul(this.scale).add(this.scrollX, this.scrollZ).add(this.cameraOffsetX, this.cameraOffsetZ).div(2000.0D);
				var info = getCloudTypeAt(uv, this.types);
				Color color = Color.getHSBColor((float)info.getLeft() / (float)this.types.length, 1.0F, 1.0F);
				float fade = info.getRight().floatValue();
				this.textureBuffer.putFloat(index     , (float)color.getRed() / 255.0F * fade);
				this.textureBuffer.putFloat(index + 4 , (float)color.getGreen() / 255.0F * fade);
				this.textureBuffer.putFloat(index + 8 , (float)color.getBlue() / 255.0F * fade);
				this.textureBuffer.putFloat(index + 12, 1.0F);
//				this.textureBuffer.putFloat(index, 0.0F);
//				this.textureBuffer.putFloat(index + 4, 0.0F);
//				this.textureBuffer.putFloat(index + 8, 1.0F);
//				this.textureBuffer.putFloat(index + 12, 1.0F);
			}
		}
	}
	
	public int getTextureId()
	{
		return this.textureId;
	}
	
	private static double hash12(Vector2d p)
	{
		Vector3d p3 = new Vector3d(p.x * 0.1031D, p.y * 0.1031D, p.x * 0.1031D);
		p3.sub(p3.floor(new Vector3d()));
		double dot = p3.dot(new Vector3d(p3.y + 33.33F, p3.z + 33.33F, p3.x + 33.33F));
		p3.add(dot, dot, dot);
		double finalResult = (p3.x + p3.y) * p3.z;
		return finalResult - Math.floor(finalResult);
	}
	
	public static Pair<Integer, Double> getCloudTypeAt(Vector2d pos, CloudType[] types) 
	{
		Vector2d indexUv = pos.floor(new Vector2d());
		Vector2d fractUv = new Vector2d(pos).sub(indexUv);

		double minimumDist = 8.0D;  
		Vector2d closestCoord = null;
		Vector2d closestPoint = null;
		for (double y = -1.0D; y <= 1.0D; y++) 
		{
			for (double x = -1.0D; x <= 1.0D; x++) 
			{
				Vector2d neighbor = new Vector2d(x, y);
	            Vector2d point = new Vector2d(hash12(new Vector2d(indexUv).add(neighbor)) * 1.0D);
				Vector2d coord = new Vector2d(neighbor).add(point).sub(fractUv);
				double dist = coord.length();
				if (dist < minimumDist) 
				{
					minimumDist = dist;
	                closestCoord = coord;
	                closestPoint = point;
				}
			}
		}
	    minimumDist = 8.0D;
	    for (double y = -1.0D; y <= 1.0D; y++) 
		{
			for (double x = -1.0D; x <= 1.0D; x++) 
			{
				Vector2d neighbor = new Vector2d(x, y);
				Vector2d point = new Vector2d(hash12(new Vector2d(indexUv).add(neighbor)) * 1.0D);
				Vector2d coord = new Vector2d(neighbor).add(point).sub(fractUv);
				if (closestCoord.distance(coord) > 0.0D)
				{
					Vector2d firstTerm = new Vector2d(closestCoord).add(coord).mul(0.5D);
					Vector2d secondTerm = new Vector2d(coord).sub(closestCoord).normalize();
					double dot = firstTerm.dot(secondTerm);
					minimumDist = Math.min(minimumDist, dot);
				}
			}
		}
	    int index = (int)Math.floor(hash12(closestPoint) * (double)types.length);
	    double fade = 1.0D - Math.min(minimumDist * 3.0D, 1.0D);
	    return Pair.of(index, fade);
	}
	
	private static Vector2d round(Vector2d vec)
	{
		vec.x = Math.round(vec.x * 10.0D) / 10.0D;
		vec.y = Math.round(vec.y * 10.0D) / 10.0D;
		return vec;
	}
}
