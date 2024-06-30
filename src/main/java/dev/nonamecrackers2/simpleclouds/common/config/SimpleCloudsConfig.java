package dev.nonamecrackers2.simpleclouds.common.config;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraftforge.common.ForgeConfigSpec;
import nonamecrackers2.crackerslib.common.config.ConfigHelper;

public class SimpleCloudsConfig
{
	public static final ClientConfig CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;
	
	static
	{
		var clientPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
		CLIENT = clientPair.getLeft();
		CLIENT_SPEC = clientPair.getRight();
	}
	
	public static class ClientConfig extends ConfigHelper
	{
		public final ForgeConfigSpec.ConfigValue<Boolean> showCloudPreviewerInfoPopup;
		public final ForgeConfigSpec.ConfigValue<Double> noiseThreshold;
		public final ForgeConfigSpec.ConfigValue<Boolean> movementSmoothing;
		public final ForgeConfigSpec.ConfigValue<Double> speedModifier;
		
		public ClientConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			this.showCloudPreviewerInfoPopup = this.createValue(true, "showCloudPreviewerInfoPopup", false, "Specifies if the info pop-up should appear when opening the cloud previewer menu");
			this.noiseThreshold = this.createRangedDoubleValue(0.5D, 0.0D, 1.0D, "noiseThreshold", false, "Specifies the noise threshold for the clouds. Impacts how big each individual cloud is");
			this.movementSmoothing = this.createValue(false, "movementSmoothing", false, "Specifies if some movement smoothing should be applied to the clouds so they don't look as distracting. May impact performance");
			this.speedModifier = this.createRangedDoubleValue(1.0D, 0.1D, 10.0D, "speedModifier", false, "Specifies the movement speed of the clouds");
		}
	}
}
