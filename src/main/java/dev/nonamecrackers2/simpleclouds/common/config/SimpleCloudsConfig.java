package dev.nonamecrackers2.simpleclouds.common.config;

import java.util.List;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudStyle;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.world.FogRenderMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import nonamecrackers2.crackerslib.common.config.ConfigHelper;

public class SimpleCloudsConfig
{
	public static final ClientConfig CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final CommonConfig COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final ServerConfig SERVER;
	public static final ForgeConfigSpec SERVER_SPEC;
	
	static
	{
		var clientPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
		CLIENT = clientPair.getLeft();
		CLIENT_SPEC = clientPair.getRight();
		var commonPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
		COMMON = commonPair.getLeft();
		COMMON_SPEC = commonPair.getRight();
		var serverPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
		SERVER = serverPair.getLeft();
		SERVER_SPEC = serverPair.getRight();
	}
	
	public static class ClientConfig extends ConfigHelper
	{
		public final ForgeConfigSpec.ConfigValue<Boolean> showCloudPreviewerInfoPopup;
		public final ForgeConfigSpec.ConfigValue<Integer> cloudHeight;
		public final ForgeConfigSpec.ConfigValue<Double> speedModifier;
		public final ForgeConfigSpec.ConfigValue<Integer> framesToGenerateMesh;
		public final ForgeConfigSpec.ConfigValue<Boolean> testSidesThatAreOccluded;
		public final ForgeConfigSpec.ConfigValue<Boolean> renderStormFog;
		public final ForgeConfigSpec.ConfigValue<LevelOfDetailOptions> levelOfDetail;
		public final ForgeConfigSpec.ConfigValue<Boolean> frustumCulling;
		public final ForgeConfigSpec.ConfigValue<Double> stormFogAngle;
		public final ForgeConfigSpec.ConfigValue<Boolean> renderClouds;
		public final ForgeConfigSpec.ConfigValue<Boolean> generateMesh;
		public final ForgeConfigSpec.ConfigValue<CloudMode> cloudMode;
		public final ForgeConfigSpec.ConfigValue<String> singleModeCloudType;
		public final ForgeConfigSpec.ConfigValue<Integer> singleModeFadeStartPercentage;
		public final ForgeConfigSpec.ConfigValue<Integer> singleModeFadeEndPercentage;
		public final ForgeConfigSpec.ConfigValue<CloudStyle> cloudStyle;
		public final ForgeConfigSpec.ConfigValue<Long> cloudSeed;
		public final ForgeConfigSpec.ConfigValue<Boolean> useSpecificSeed;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;
		public final ForgeConfigSpec.ConfigValue<Boolean> whitelistAsBlacklist;
		public final ForgeConfigSpec.ConfigValue<FogRenderMode> fogMode;
		public final ForgeConfigSpec.ConfigValue<Boolean> lightningColorVariation;
		public final ForgeConfigSpec.ConfigValue<Double> rainAngle;
		
		public ClientConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.cloudMode = this.createEnumValue(CloudMode.AMBIENT, "clientSideCloudMode", false, "Specifies how the clouds should behave in a client-side only context. SINGLE uses only a single cloud type. AMBIENT carves clouds around the player, keeping them at a distance. Due to be on a client-side only context, DEFAULT can not be picked and the vanilla weather system will be used. If Simple Clouds is installed on a server, this option will be ignored and the client will instead use the option set by the server", CloudMode.AMBIENT, CloudMode.SINGLE);
			
			this.cloudStyle = this.createEnumValue(CloudStyle.DEFAULT, "cloudStyle", false, "Specifies the visual style of the cloud. DEFAULT is the default style. SHADED adds minimal shading to clouds, making them appear more defined");
			
			this.showCloudPreviewerInfoPopup = this.createValue(true, "showCloudPreviewerInfoPopup", false, "Specifies if the info pop-up should appear when opening the cloud previewer menu");
			
			this.speedModifier = this.createRangedDoubleValue(1.0D, 0.1D, 32.0D, "clientSideSpeedModifier", false, "Specifies the movement speed of the clouds");
			
			this.cloudHeight = this.createRangedIntValue(128, CloudManager.CLOUD_HEIGHT_MIN, CloudManager.CLOUD_HEIGHT_MAX, "clientSideCloudHeight", false, "Specifies the render Y offset for the clouds");
			
			this.stormFogAngle = this.createRangedDoubleValue(80.0D, 50.0D, 90.0D, "stormFogAngle", false, "Specifies the angle parellel to the horizon that the storm fog should be directed to");
			
			this.dimensionWhitelist = this.createListValue(String.class, () -> {
				return Lists.newArrayList("minecraft:overworld");
			}, val -> {
				return ResourceLocation.isValidResourceLocation(val);
			}, "dimensionWhitelist", false, "Specifies the allowed dimensions that Simple Clouds is active in");
			
			this.whitelistAsBlacklist = this.createValue(false, "whitelistAsBlacklist", false, "Specifies if the dimension whitelist should instead be use as a blacklist");
			
			this.fogMode = this.createEnumValue(FogRenderMode.SCREEN_SPACE, "fogMode", false, "Specifies the type of world fog that should be used. Each has their own advantages and disadvantages, ranging from visual discrepancies to possible compatibility issues");
			
			this.lightningColorVariation = this.createValue(true, "lightningColorVariation", false, "Specifies if lightning should have slight random color variation");
			
			this.rainAngle = this.createRangedDoubleValue(15.0D, 0.0D, 45.0D, "rainAngle", false, "Specifies the angle of the rain, perpendicular to the ground. Higher values makes it more horizontal");
			
			builder.comment("Seed").push("seed");
			
			this.cloudSeed = this.createValue(0L, "cloudSeed", false, "Specifies the seed to use for the clouds. Will apply for all servers that the user connects to with the mod on the client-side only");
			
			this.useSpecificSeed = this.createValue(false, "useSpecificSeed", false, "Specifies if the seed set by the 'Cloud Seed' option should be used or not");
			
			builder.pop();
					
			builder.comment("Performance").push("performance");
			
			this.framesToGenerateMesh = this.createRangedIntValue(3, 1, 32, "framesToGenerateMesh", false, "Specifies how many frames it should take to generate the entire cloud mesh. Higher values will improve performance at the cost of some visual artifacts");
			
			this.testSidesThatAreOccluded = this.createValue(false, "testSidesThatAreOccluded", false, "Specifies if faces that are not visible to the camera should be tested during mesh generation. Settings this to off can improve performance at the cost of some visual artifacts");
			
			this.renderStormFog = this.createValue(true, "renderStormFog", false, "Specifies if the fog beneath storm clouds should appear or not. Disabling can improve performance");
			
			this.levelOfDetail = this.createEnumValue(LevelOfDetailOptions.HIGH, "levelOfDetail", false, "Specifies the quality of the level of detail");
			
			this.frustumCulling = this.createValue(true, "frustumCulling", false, "Culls cloud chunks not visible to the player. Disable if facing noticeable artifacts with high cloud mesh generate times");
			
			builder.pop();
			
			builder.comment("Debug").push("debug");
			
			this.renderClouds = this.createValue(true, "renderClouds", false, "Toggles rendering of the clouds");
			
			this.generateMesh = this.createValue(true, "generateMesh", false, "Toggles the generation of the cloud mesh");
			
			builder.pop();
			
			builder.comment("Single Mode").push("single_mode");
			
			this.singleModeCloudType = this.createValue("simpleclouds:itty_bitty", "clientSideSingleModeCloudType", false, "Specifies the cloud type that should be used when the SINGLE cloud mode is active");
			
			this.singleModeFadeStartPercentage = this.createRangedIntValue(80, 0, 100, "singleModeFadeStartPercentage", false, "Specifies the percentage of the cloud render distance that the clouds should begin to fade away, when using the single cloud type mode (e.x. 50 would start to make the clouds fade away at half of the cloud render distance)");
			
			this.singleModeFadeEndPercentage = this.createRangedIntValue(100, 0, 100, "singleModeFadeEndPercentage", false, "Specifies the percentage of the cloud render distance that the clouds will be fully faded away, when using the single cloud type mode (e.x. 50 would make the clouds completely disappear past half the cloud render distance)");
			
			builder.pop();
		}
	}
	
	public static class CommonConfig extends ConfigHelper
	{
		public final ForgeConfigSpec.ConfigValue<Integer> lightningSpawnIntervalMin;
		public final ForgeConfigSpec.ConfigValue<Integer> lightningSpawnIntervalMax;
		
		public CommonConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			builder.comment("Weather").push("weather");
			
			builder.comment("Lightning And Thunder").push("lightning_and_thunder");
			
			this.lightningSpawnIntervalMin = this.createRangedIntValue(10, 1, 72000, "lightningSpawnIntervalMinimum", false, "Specifies the shortest interval until the next lightning strike will spawn, in ticks");
			
			this.lightningSpawnIntervalMax = this.createRangedIntValue(160, 1, 72000, "lightningSpawnIntervalMaximum", false, "Specifies the longest interval until the next lightning strike will spawn, in ticks");
			
			builder.pop();
			
			builder.pop();
		}
	}
	
	public static class ServerConfig extends ConfigHelper
	{
		public final ForgeConfigSpec.ConfigValue<CloudMode> cloudMode;
		public final ForgeConfigSpec.ConfigValue<String> singleModeCloudType;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;
		public final ForgeConfigSpec.ConfigValue<Boolean> whitelistAsBlacklist;
		
		public ServerConfig(ForgeConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.dimensionWhitelist = this.createListValue(String.class, () -> {
				return Lists.newArrayList("minecraft:overworld");
			}, val -> {
				return ResourceLocation.isValidResourceLocation(val);
			}, "dimensionWhitelist", true, "Specifies the allowed dimensions that Simple Clouds is active in");
			
			this.whitelistAsBlacklist = this.createValue(false, "whitelistAsBlacklist", true, "Specifies if the dimension whitelist should instead be use as a blacklist");
			
			this.cloudMode = this.createEnumValue(CloudMode.DEFAULT, "cloudMode", false, "Specifies how the clouds should behave. DEFAULT uses all cloud types with the default weather in Simple Clouds. SINGLE uses only a single cloud type and its associated weather. AMBIENT disables localized weather and carves clouds around the player, keeping them at a distance");
			
			builder.comment("Single Mode").push("single_mode");
			
			this.singleModeCloudType = this.createValue("simpleclouds:itty_bitty", "singleModeCloudType", false, "Specifies the cloud type that should be used when the SINGLE cloud mode is active");
			
			builder.pop();
		}
	}
}
