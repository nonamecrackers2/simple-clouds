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
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.RestartType;
import nonamecrackers2.crackerslib.common.config.ConfigHelper;

public class SimpleCloudsConfig
{
	public static final ClientConfig CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;
	public static final CommonConfig COMMON;
	public static final ModConfigSpec COMMON_SPEC;
	public static final ServerConfig SERVER;
	public static final ModConfigSpec SERVER_SPEC;
	
	static
	{
		var clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
		CLIENT = clientPair.getLeft();
		CLIENT_SPEC = clientPair.getRight();
		var commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
		COMMON = commonPair.getLeft();
		COMMON_SPEC = commonPair.getRight();
		var serverPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
		SERVER = serverPair.getLeft();
		SERVER_SPEC = serverPair.getRight();
	}
	
	public static class ClientConfig extends ConfigHelper
	{
		public final ModConfigSpec.ConfigValue<Boolean> showCloudPreviewerInfoPopup;
		public final ModConfigSpec.ConfigValue<Integer> cloudHeight;
		public final ModConfigSpec.ConfigValue<Double> speedModifier;
		public final ModConfigSpec.ConfigValue<Integer> framesToGenerateMesh;
		public final ModConfigSpec.ConfigValue<Boolean> testSidesThatAreOccluded;
		public final ModConfigSpec.ConfigValue<Boolean> renderStormFog;
		public final ModConfigSpec.ConfigValue<LevelOfDetailOptions> levelOfDetail;
		public final ModConfigSpec.ConfigValue<Boolean> frustumCulling;
		public final ModConfigSpec.ConfigValue<Double> stormFogAngle;
		public final ModConfigSpec.ConfigValue<Boolean> renderClouds;
		public final ModConfigSpec.ConfigValue<Boolean> generateMesh;
		public final ModConfigSpec.ConfigValue<CloudMode> cloudMode;
		public final ModConfigSpec.ConfigValue<String> singleModeCloudType;
		public final ModConfigSpec.ConfigValue<Integer> singleModeFadeStartPercentage;
		public final ModConfigSpec.ConfigValue<Integer> singleModeFadeEndPercentage;
		public final ModConfigSpec.ConfigValue<CloudStyle> cloudStyle;
		public final ModConfigSpec.ConfigValue<Long> cloudSeed;
		public final ModConfigSpec.ConfigValue<Boolean> useSpecificSeed;
		public final ModConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;
		public final ModConfigSpec.ConfigValue<Boolean> whitelistAsBlacklist;
		public final ModConfigSpec.ConfigValue<FogRenderMode> fogMode;
		public final ModConfigSpec.ConfigValue<Boolean> lightningColorVariation;
		public final ModConfigSpec.ConfigValue<Double> rainAngle;
		public final ModConfigSpec.ConfigValue<Integer> thunderAttenuationDistance;
		
		public ClientConfig(ModConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.cloudMode = this.createEnumValue(CloudMode.AMBIENT, "clientSideCloudMode", RestartType.NONE, "Specifies how the clouds should behave in a client-side only context. SINGLE uses only a single cloud type. AMBIENT carves clouds around the player, keeping them at a distance. Due to be on a client-side only context, DEFAULT can not be picked and the vanilla weather system will be used. If Simple Clouds is installed on a server, this option will be ignored and the client will instead use the option set by the server", CloudMode.AMBIENT, CloudMode.SINGLE);
			
			this.cloudStyle = this.createEnumValue(CloudStyle.DEFAULT, "cloudStyle", RestartType.NONE, "Specifies the visual style of the cloud. DEFAULT is the default style. SHADED adds minimal shading to clouds, making them appear more defined");
			
			this.showCloudPreviewerInfoPopup = this.createValue(true, "showCloudPreviewerInfoPopup", RestartType.NONE, "Specifies if the info pop-up should appear when opening the cloud previewer menu");
			
			this.speedModifier = this.createRangedDoubleValue(1.0D, 0.1D, 32.0D, "clientSideSpeedModifier", RestartType.NONE, "Specifies the movement speed of the clouds");
			
			this.cloudHeight = this.createRangedIntValue(128, CloudManager.CLOUD_HEIGHT_MIN, CloudManager.CLOUD_HEIGHT_MAX, "clientSideCloudHeight", RestartType.NONE, "Specifies the render Y offset for the clouds");
			
			this.stormFogAngle = this.createRangedDoubleValue(80.0D, 50.0D, 90.0D, "stormFogAngle", RestartType.NONE, "Specifies the angle parellel to the horizon that the storm fog should be directed to");
			
			this.dimensionWhitelist = this.createListValue(String.class, () -> {
				return Lists.newArrayList("minecraft:overworld");
			}, val -> {
				return ResourceLocation.tryParse(val) != null;
			}, "dimensionWhitelist", RestartType.NONE, "Specifies the allowed dimensions that Simple Clouds is active in", "minecraft:dimension");
			
			this.whitelistAsBlacklist = this.createValue(false, "whitelistAsBlacklist", RestartType.NONE, "Specifies if the dimension whitelist should instead be use as a blacklist");
			
			this.fogMode = this.createEnumValue(FogRenderMode.SCREEN_SPACE, "fogMode", RestartType.NONE, "Specifies the type of world fog that should be used. Each has their own advantages and disadvantages, ranging from visual discrepancies to possible compatibility issues");
			
			this.lightningColorVariation = this.createValue(true, "lightningColorVariation", RestartType.NONE, "Specifies if lightning should have slight random color variation");
			
			this.rainAngle = this.createRangedDoubleValue(15.0D, 0.0D, 45.0D, "rainAngle", RestartType.NONE, "Specifies the angle of the rain, perpendicular to the ground. Higher values makes it more horizontal");
			
			this.thunderAttenuationDistance = this.createRangedIntValue(2000, 100, 20000, "thunderAttenuationDistance", RestartType.NONE, "Specifies the attenuation distance for thunder. The lower the value, the quieter it will be from longer distances");
			
			builder.comment("Seed").push("seed");
			
			this.cloudSeed = this.createValue(0L, "cloudSeed", RestartType.NONE, "Specifies the seed to use for the clouds. Will apply for all servers that the user connects to with the mod on the client-side only");
			
			this.useSpecificSeed = this.createValue(false, "useSpecificSeed", RestartType.NONE, "Specifies if the seed set by the 'Cloud Seed' option should be used or not");
			
			builder.pop();
					
			builder.comment("Performance").push("performance");
			
			this.framesToGenerateMesh = this.createRangedIntValue(3, 1, 32, "framesToGenerateMesh", RestartType.NONE, "Specifies how many frames it should take to generate the entire cloud mesh. Higher values will improve performance at the cost of some visual artifacts");
			
			this.testSidesThatAreOccluded = this.createValue(false, "testSidesThatAreOccluded", RestartType.NONE, "Specifies if faces that are not visible to the camera should be tested during mesh generation. Settings this to off can improve performance at the cost of some visual artifacts");
			
			this.renderStormFog = this.createValue(true, "renderStormFog", RestartType.NONE, "Specifies if the fog beneath storm clouds should appear or not. Disabling can improve performance");
			
			this.levelOfDetail = this.createEnumValue(LevelOfDetailOptions.HIGH, "levelOfDetail", RestartType.NONE, "Specifies the quality of the level of detail");
			
			this.frustumCulling = this.createValue(true, "frustumCulling", RestartType.NONE, "Culls cloud chunks not visible to the player. Disable if facing noticeable artifacts with high cloud mesh generate times");
			
			builder.pop();
			
			builder.comment("Debug").push("debug");
			
			this.renderClouds = this.createValue(true, "renderClouds", RestartType.NONE, "Toggles rendering of the clouds");
			
			this.generateMesh = this.createValue(true, "generateMesh", RestartType.NONE, "Toggles the generation of the cloud mesh");
			
			builder.pop();
			
			builder.comment("Single Mode").push("single_mode");
			
			this.singleModeCloudType = this.createValue("simpleclouds:itty_bitty", "clientSideSingleModeCloudType", RestartType.NONE, "Specifies the cloud type that should be used when the SINGLE cloud mode is active");
			
			this.singleModeFadeStartPercentage = this.createRangedIntValue(80, 0, 100, "singleModeFadeStartPercentage", RestartType.NONE, "Specifies the percentage of the cloud render distance that the clouds should begin to fade away, when using the single cloud type mode (e.x. 50 would start to make the clouds fade away at half of the cloud render distance)");
			
			this.singleModeFadeEndPercentage = this.createRangedIntValue(100, 0, 100, "singleModeFadeEndPercentage", RestartType.NONE, "Specifies the percentage of the cloud render distance that the clouds will be fully faded away, when using the single cloud type mode (e.x. 50 would make the clouds completely disappear past half the cloud render distance)");
			
			builder.pop();
		}
	}
	
	public static class CommonConfig extends ConfigHelper
	{
		public final ModConfigSpec.ConfigValue<Integer> lightningSpawnIntervalMin;
		public final ModConfigSpec.ConfigValue<Integer> lightningSpawnIntervalMax;
		
		public CommonConfig(ModConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			builder.comment("Weather").push("weather");
			
			builder.comment("Lightning And Thunder").push("lightning_and_thunder");
			
			this.lightningSpawnIntervalMin = this.createRangedIntValue(10, 1, 72000, "lightningSpawnIntervalMinimum", RestartType.NONE, "Specifies the shortest interval until the next lightning strike will spawn, in ticks");
			
			this.lightningSpawnIntervalMax = this.createRangedIntValue(160, 1, 72000, "lightningSpawnIntervalMaximum", RestartType.NONE, "Specifies the longest interval until the next lightning strike will spawn, in ticks");
			
			builder.pop();
			
			builder.pop();
		}
	}
	
	public static class ServerConfig extends ConfigHelper
	{
		public final ModConfigSpec.ConfigValue<CloudMode> cloudMode;
		public final ModConfigSpec.ConfigValue<String> singleModeCloudType;
		public final ModConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;
		public final ModConfigSpec.ConfigValue<Boolean> whitelistAsBlacklist;
		
		public ServerConfig(ModConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.dimensionWhitelist = this.createListValue(String.class, () -> {
				return Lists.newArrayList("minecraft:overworld");
			}, val -> {
				return ResourceLocation.tryParse(val) != null;
			}, "dimensionWhitelist", RestartType.WORLD, "Specifies the allowed dimensions that Simple Clouds is active in", "minecraft:dimension");
			
			this.whitelistAsBlacklist = this.createValue(false, "whitelistAsBlacklist", RestartType.WORLD, "Specifies if the dimension whitelist should instead be use as a blacklist");
			
			this.cloudMode = this.createEnumValue(CloudMode.DEFAULT, "cloudMode", RestartType.NONE, "Specifies how the clouds should behave. DEFAULT uses all cloud types with the default weather in Simple Clouds. SINGLE uses only a single cloud type and its associated weather. AMBIENT disables localized weather and carves clouds around the player, keeping them at a distance");
			
			builder.comment("Single Mode").push("single_mode");
			
			this.singleModeCloudType = this.createValue("simpleclouds:itty_bitty", "singleModeCloudType", RestartType.NONE, "Specifies the cloud type that should be used when the SINGLE cloud mode is active");
			
			builder.pop();
		}
	}
}
