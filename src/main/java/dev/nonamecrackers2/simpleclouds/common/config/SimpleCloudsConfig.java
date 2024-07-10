package dev.nonamecrackers2.simpleclouds.common.config;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
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
		public final ForgeConfigSpec.ConfigValue<Integer> cloudHeight;
		public final ForgeConfigSpec.ConfigValue<Double> speedModifier;
		public final ForgeConfigSpec.ConfigValue<Integer> cloudMeshGenerateTime;
		public final ForgeConfigSpec.ConfigValue<Boolean> testSidesThatAreOccluded;
		public final ForgeConfigSpec.ConfigValue<Boolean> renderStormFog;
		public final ForgeConfigSpec.ConfigValue<LevelOfDetailOptions> levelOfDetail;
		public final ForgeConfigSpec.ConfigValue<Boolean> frustumCulling;
		public final ForgeConfigSpec.ConfigValue<Double> stormFogAngle;
		public final ForgeConfigSpec.ConfigValue<Boolean> renderClouds;
		public final ForgeConfigSpec.ConfigValue<Boolean> generateMesh;
		
		public ClientConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.showCloudPreviewerInfoPopup = this.createValue(true, "showCloudPreviewerInfoPopup", false, "Specifies if the info pop-up should appear when opening the cloud previewer menu");
			
			this.speedModifier = this.createRangedDoubleValue(1.0D, 0.1D, 32.0D, "speedModifier", false, "Specifies the movement speed of the clouds");
			
			this.cloudHeight = this.createRangedIntValue(128, 0, 2048, "cloudHeight", false, "Specifies the render Y offset for the clouds");
			
			this.stormFogAngle = this.createRangedDoubleValue(80.0D, 50.0D, 90.0D, "stormFogAngle", false, "Specifies the angle parellel to the horizon that the storm fog should be directed to");
			
			builder.comment("Performance").push("performance");
			
			this.cloudMeshGenerateTime = this.createRangedIntValue(3, 1, 32, "cloudMeshGenerateTime", false, "Specifies how many frames it should take to generate the entire cloud mesh. Higher values will improve performance at the cost of some visual artifacts");
			
			this.testSidesThatAreOccluded = this.createValue(false, "testSidesThatAreOccluded", false, "Specifies if faces that are not visible to the camera should be tested during mesh generation. Settings this to off can improve performance at the cost of some visual artifacts");
			
			this.renderStormFog = this.createValue(true, "renderStormFog", false, "Specifies if the fog beneath storm clouds should appear or not. Disabling can improve performance");
			
			this.levelOfDetail = this.createEnumValue(LevelOfDetailOptions.HIGH, "levelOfDetail", false, "Specifies the quality of the level of detail");
			
			this.frustumCulling = this.createValue(true, "frustumCulling", false, "Culls cloud chunks not visible to the player. Disable if facing noticeable artifacts with high cloud mesh generate times");
			
			builder.pop();
			
			builder.comment("Debug").push("debug");
			
			this.renderClouds = this.createValue(true, "renderClouds", false, "Toggles rendering of the clouds");
			
			this.generateMesh = this.createValue(true, "generateMesh", false, "Toggles the generation of the cloud mesh");
			
			builder.pop();
		}
	}
}
