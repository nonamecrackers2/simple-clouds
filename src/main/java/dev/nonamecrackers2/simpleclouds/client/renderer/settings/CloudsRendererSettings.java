package dev.nonamecrackers2.simpleclouds.client.renderer.settings;

import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public abstract class CloudsRendererSettings
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudsRendererSettings");
	public static final CloudsRendererSettings DEFAULT = new CloudsRendererSettings()
	{
		@Override
		public boolean useTransparency()
		{
			return SimpleCloudsConfig.CLIENT.transparency.get();
		}
		
		@Override
		public boolean shadedClouds()
		{
			return SimpleCloudsConfig.CLIENT.shadedClouds.get();
		}
		
		@Override
		public boolean useFixedMeshDataSectionSize()
		{
			return SimpleCloudsConfig.CLIENT.concurrentComputeDispatches.get();
		}
		
		@Override
		public LevelOfDetailOptions getLodConfig()
		{
			return SimpleCloudsConfig.CLIENT.levelOfDetail.get();
		}
		
		@Override
		public CloudMode getCloudMode()
		{
			ClientLevel level = Minecraft.getInstance().level;
			if (level != null)
				return CloudManager.get(level).getCloudMode();
			else
				return CloudMode.DEFAULT;
		}
		
		@Override
		public @Nullable CloudType getSingleModeCloudType()
		{
			String rawId;
			ClientLevel level = Minecraft.getInstance().level;
			if (level != null)
				rawId = CloudManager.get(level).getSingleModeCloudTypeRawId();
			else
				rawId = "simpleclouds:itty_bitty";
			return ClientSideCloudTypeManager.getInstance().getCloudTypeFromRawId(rawId).orElse(null);
		}
	};
	
	private @Nullable CloudMode currentCloudMode;
	private @Nullable LevelOfDetailOptions currentLod;
	
	public abstract CloudMode getCloudMode();
	
	public abstract boolean shadedClouds();
	
	public abstract boolean useTransparency();
	
	public abstract boolean useFixedMeshDataSectionSize();
	
	public abstract LevelOfDetailOptions getLodConfig();
	
	public abstract @Nullable CloudType getSingleModeCloudType();
	
	public boolean needsReinitialization(@Nullable CloudMeshGenerator generator)
	{
		return this.checkAndOrBeginInitialization(generator, false);
	}
	
	public boolean checkAndOrBeginInitialization(@Nullable CloudMeshGenerator generator)
	{
		return this.checkAndOrBeginInitialization(generator, true);
	}
	
	protected boolean checkAndOrBeginInitialization(@Nullable CloudMeshGenerator generator, boolean initializesAfterwards)
	{
		boolean flag = false;
		
		CloudMode mode = Objects.requireNonNull(this.getCloudMode(), "Must supply a cloud mode");
		boolean shadedClouds = this.shadedClouds();
		boolean transparency = this.useTransparency();
		LevelOfDetailOptions lod = Objects.requireNonNull(this.getLodConfig(), "Must supply a LOD");
		boolean fixedMeshDataSectionSize = this.useFixedMeshDataSectionSize();
		
		if (generator != null)
		{
			if (this.currentCloudMode != mode)
				flag = true;
			else if (generator.shadedCloudsEnabled() != shadedClouds)
				flag = true;
			else if (generator.transparencyEnabled() != transparency)
				flag = true;
			else if (this.currentLod != lod)
				flag = true;
			else if (generator.usesFixedMeshDataSectionSize() != fixedMeshDataSectionSize)
				flag = true;
		}
		else
		{
			flag = true;
		}
		
		if (flag && initializesAfterwards)
		{
			this.currentCloudMode = mode;
			this.currentLod = lod;
			
			LOGGER.debug("Beginning mesh generator initialization for cloud mode {}, shaded clouds {}, transparency {}, and LOD {}", mode, shadedClouds, transparency, lod);
		}
		
		return flag;
	}
	
	public @Nullable CloudMode getCurrentCloudMode()
	{
		return this.currentCloudMode;
	}
	
	public @Nullable LevelOfDetailOptions getCurrentLod()
	{
		return this.currentLod;
	}
}
