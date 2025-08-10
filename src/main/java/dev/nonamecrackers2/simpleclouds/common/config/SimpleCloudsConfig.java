package dev.nonamecrackers2.simpleclouds.common.config;

import java.util.List;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.GenerationInterval;
import dev.nonamecrackers2.simpleclouds.client.world.FogRenderMode;
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
		public final ModConfigSpec.ConfigValue<Long> cloudSeed;
		public final ModConfigSpec.ConfigValue<Boolean> useSpecificSeed;
		public final ModConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;
		public final ModConfigSpec.ConfigValue<Boolean> whitelistAsBlacklist;
		public final ModConfigSpec.ConfigValue<FogRenderMode> fogMode;
		public final ModConfigSpec.ConfigValue<Boolean> lightningColorVariation;
		public final ModConfigSpec.ConfigValue<Double> rainAngle;
		public final ModConfigSpec.ConfigValue<Integer> thunderAttenuationDistance;
		public final ModConfigSpec.ConfigValue<Boolean> stormFogLightningFlashes;
		public final ModConfigSpec.ConfigValue<Integer> transparencyRenderDistancePercentage;
		public final ModConfigSpec.ConfigValue<Boolean> concurrentComputeDispatches;
		public final ModConfigSpec.ConfigValue<GenerationInterval> generationInterval;
		public final ModConfigSpec.ConfigValue<Integer> targetMeshGenFps;
		public final ModConfigSpec.ConfigValue<Boolean> customRainSounds;
		public final ModConfigSpec.ConfigValue<Boolean> renderCustomRain;
		//Cloud Visuals
		public final ModConfigSpec.ConfigValue<Boolean> cubeNormals;
		public final ModConfigSpec.ConfigValue<Boolean> shadedClouds;
		public final ModConfigSpec.ConfigValue<Boolean> transparency;
		public final ModConfigSpec.ConfigValue<Boolean> atmosphericClouds;
		//Distant Horizons
		public final ModConfigSpec.ConfigValue<Boolean> distantShadows;
		public final ModConfigSpec.ConfigValue<Integer> shadowDistance;
		//Vivecraft
		public final ModConfigSpec.ConfigValue<Boolean> showVivecraftNotice;
		
		public ClientConfig(ModConfigSpec.Builder builder)
		{
			super(builder, SimpleCloudsMod.MODID);
			
			this.cloudMode = this.createEnumValue(CloudMode.AMBIENT, "clientSideCloudMode", RestartType.NONE, "Specifies how the clouds should behave in a client-side only context. SINGLE uses only a single cloud type. AMBIENT carves clouds around the player, keeping them at a distance. Due to be on a client-side only context, DEFAULT can not be picked and the vanilla weather system will be used. If Simple Clouds is installed on a server, this option will be ignored and the client will instead use the option set by the server", CloudMode.AMBIENT, CloudMode.SINGLE);
			
			this.showCloudPreviewerInfoPopup = this.createValue(true, "showCloudPreviewerInfoPopup", RestartType.NONE, "Specifies if the info pop-up should appear when opening the cloud previewer menu");
			
			this.speedModifier = this.createRangedDoubleValue(1.0D, 0.1D, 32.0D, "clientSideSpeedModifier", RestartType.NONE, "Specifies the movement speed of the clouds");
			
			this.cloudHeight = this.createRangedIntValue(128, CloudManager.CLOUD_HEIGHT_MIN, CloudManager.CLOUD_HEIGHT_MAX, "clientSideCloudHeight", RestartType.NONE, "Specifies the render Y offset for the clouds");
			
			this.dimensionWhitelist = this.createListValue(String.class, () -> {
				return Lists.newArrayList("minecraft:overworld");
			}, val -> {
				return ResourceLocation.tryParse(val) != null;
			}, "dimensionWhitelist", RestartType.NONE, "Specifies the allowed dimensions that Simple Clouds is active in", "minecraft:dimension");
			
			this.whitelistAsBlacklist = this.createValue(false, "whitelistAsBlacklist", RestartType.NONE, "Specifies if the dimension whitelist should instead be use as a blacklist");
			
			builder.comment("Preference").push("preference");
			
			this.fogMode = this.createEnumValue(FogRenderMode.SCREEN_SPACE, "fogMode", RestartType.NONE, "Specifies the type of world fog that should be used. Each has their own advantages and disadvantages, ranging from visual discrepancies to possible compatibility issues");
			
			this.rainAngle = this.createRangedDoubleValue(15.0D, 0.0D, 45.0D, "rainAngle", RestartType.NONE, "Specifies the angle of the rain, perpendicular to the ground. Higher values makes it more horizontal");
			
			this.stormFogAngle = this.createRangedDoubleValue(80.0D, 50.0D, 90.0D, "stormFogAngle", RestartType.NONE, "Specifies the angle parellel to the horizon that the storm fog should be directed to");
			
			this.lightningColorVariation = this.createValue(true, "lightningColorVariation", RestartType.NONE, "Specifies if lightning should have slight random color variation");
			
			this.thunderAttenuationDistance = this.createRangedIntValue(2000, 100, 20000, "thunderAttenuationDistance", RestartType.NONE, "Specifies the attenuation distance for thunder. The lower the value, the quieter it will be from longer distances");
			
			this.customRainSounds = this.createValue(true, "customRainSounds", RestartType.NONE, "Specifies if new rain sounds should replace the vanilla ones");
			
			this.renderCustomRain = this.createValue(true, "renderCustomRain", RestartType.NONE, "Specifies if custom rain rendering should be used. Automatically disabled when using Pretty/Particle Rain");
			
			builder.pop();
			
			builder.comment("Visual").push("visual");
			
			this.cubeNormals = this.createValue(false, "cubeNormals", RestartType.NONE, "Specifies if normals should be applied to each individual cube in the cloud which applies a bit of shading per cube face. Helps distinguish each individual cube in a cloud");
			
			this.shadedClouds = this.createValue(true, "shadedClouds", RestartType.NONE, "Specifies if minimal shading should be applied to clouds. May cause performance drops");
			
			this.transparency = this.createValue(true, "transparency", RestartType.NONE, "Specifies if transparent cubes should be generated for supported cloud types. May cause performance drops");
			
			this.atmosphericClouds = this.createValue(true, "atmosphericClouds", RestartType.NONE, "Specifies if a purely visual 2D atmospheric cloud layer should render");
			
			builder.pop();
			
			builder.comment("Seed").push("seed");
			
			this.cloudSeed = this.createValue(0L, "cloudSeed", RestartType.NONE, "Specifies the seed to use for the clouds. Will apply for all servers that the user connects to with the mod on the client-side only");
			
			this.useSpecificSeed = this.createValue(false, "useSpecificSeed", RestartType.NONE, "Specifies if the seed set by the 'Cloud Seed' option should be used or not");
			
			builder.pop();
					
			builder.comment("Performance").push("performance");
			
			builder.comment("Mesh Generation").push("mesh_generation");
			
			this.generationInterval = this.createEnumValue(GenerationInterval.TARGET_FPS, "generationInterval", RestartType.NONE, "How the amount of frames used to generate the entire mesh is calculated. Static will use the 'Frames To Generate Mesh' option. Dynamic will calculate it automatically depending on your FPS. Target FPS will target a certain perceived framerate for mesh generation");
			
			this.framesToGenerateMesh = this.createRangedIntValue(5, 1, 32, "framesToGenerateMesh", RestartType.NONE, "Specifies how many frames it should take to generate the entire cloud mesh. Higher values will improve performance at the cost of stuttery cloud movement");
			
			this.targetMeshGenFps = this.createRangedIntValue(30, 1, 1000, "targetMeshGenFps", RestartType.NONE, "Used to set the target FPS with the 'Target FPS' option in 'Generation Interval'");
			
			builder.pop();
			
			this.concurrentComputeDispatches = this.createValue(false, "concurrentComputeDispatches", RestartType.NONE, "EXPERIMENTAL. Uses a slightly modified algorithm that removes sync calls between chunk generator compute dispatches at the cost of higher memory usage. May result in a performance boost");
			
			this.testSidesThatAreOccluded = this.createValue(false, "testSidesThatAreOccluded", RestartType.NONE, "Specifies if faces that are not visible to the camera should be tested during mesh generation. Settings this to off can improve performance at the cost of visual issues with shadows and storm fog");
			
			this.renderStormFog = this.createValue(true, "renderStormFog", RestartType.NONE, "Specifies if 'rain' beneath storm clouds should appear or not. Disabling can improve performance, especially when in VR");
			
			this.levelOfDetail = this.createEnumValue(LevelOfDetailOptions.HIGH, "levelOfDetail", RestartType.NONE, "Specifies the quality of the level of detail. A lower setting makes clouds lower quality faster");
			
			this.frustumCulling = this.createValue(true, "frustumCulling", RestartType.NONE, "Culls cloud chunks not visible to the player. Generally should not be disabled, but can fix visual issues when looking around fast with higher frames per mesh generation values");
			
			this.stormFogLightningFlashes = this.createValue(true, "stormFogLightningFlashes", RestartType.NONE, "Toggles lightning flashes that can be seen in storm fog. Disabling can lead to potential performance gains when lightning spawns");
			
			this.transparencyRenderDistancePercentage = this.createRangedIntValue(50, 1, 100, "transparencyRenderDistancePercentage", RestartType.NONE, "Specifies the maximum percentage of the total viewable distance transparent cubes can be generated in");
			
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
			
			builder.comment("Distant Horizons").push("distant_horizons");
			
			this.distantShadows = this.createValue(true, "distantShadows", RestartType.NONE, "Toggles shadows that appear on distant terrain");
			
			this.shadowDistance = this.createRangedIntValue(5000, 500, 15000, "shadowDistance", RestartType.NONE, "Specifies the distance shadows can render");
			
			builder.pop();
			
			this.showVivecraftNotice = this.createValue(true, "showVivecraftNotice", RestartType.NONE, "Shows the Vivecraft notice on startup");
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
