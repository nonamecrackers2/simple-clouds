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
		public final ForgeConfigSpec.ConfigValue<Double> noiseThreshold;
		
		public ClientConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			this.noiseThreshold = this.createRangedDoubleValue(0.5D, 0.0D, 1.0D, "noiseThreshold", false, "Specifies the noise threshold for the clouds. Impacts how big each individual cloud is");
		}
	}
}
