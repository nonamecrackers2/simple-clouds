package dev.nonamecrackers2.simpleclouds.common.data;

import org.apache.commons.lang3.StringUtils;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.ModList;
import nonamecrackers2.crackerslib.common.util.data.ConfigLangGeneratorHelper;

public class SimpleCloudsLangProvider extends LanguageProvider
{
	public SimpleCloudsLangProvider(PackOutput output)
	{
		super(output, SimpleCloudsMod.MODID, "en_us");
	}
	
	@Override
	protected void addTranslations()
	{
		ConfigLangGeneratorHelper.langForSpec(SimpleCloudsMod.MODID, SimpleCloudsConfig.CLIENT_SPEC, this, ConfigLangGeneratorHelper.Info.ONLY_RANGE);
		ConfigLangGeneratorHelper.langForSpec(SimpleCloudsMod.MODID, SimpleCloudsConfig.COMMON_SPEC, this, ConfigLangGeneratorHelper.Info.ONLY_RANGE);
		ConfigLangGeneratorHelper.langForSpec(SimpleCloudsMod.MODID, SimpleCloudsConfig.SERVER_SPEC, this, ConfigLangGeneratorHelper.Info.ONLY_RANGE);
		this.add("gui.simpleclouds.cloud_previewer.title", "Cloud Previewer");
		this.add("gui.simpleclouds.cloud_previewer.button.title", "Cloud Previewer");
		this.add("gui.simpleclouds.cloud_previewer.button.add_layer.title", "Add Layer");
		this.add("gui.simpleclouds.cloud_previewer.button.remove_layer.title", "Remove Layer");
		this.add("gui.simpleclouds.cloud_previewer.button.toggle_preview.title", "Toggle Preview");
		this.add("gui.simpleclouds.cloud_previewer.current_layer", "Current Layer: %s");
		this.add("simpleclouds.key.openGenPreviewer", "Open Cloud Gen Previewer");
		this.add("simpleclouds.key.openDebug", "Open Debug Screen");
		this.add("simpleclouds.key.categories.main", ModList.get().getModContainerById(SimpleCloudsMod.MODID).get().getModInfo().getDisplayName());
		for (AbstractNoiseSettings.Param parameter : AbstractNoiseSettings.Param.values())
		{
			String key = "gui.simpleclouds.noise_settings.param." + parameter.toString().toLowerCase() + ".name";
			String[] splitted = parameter.toString().toLowerCase().split("_");
			for (int i = 0; i < splitted.length; i++)
				splitted[i] = StringUtils.capitalize(splitted[i]);
			this.add(key, StringUtils.join(splitted, " "));
		}
		this.add("simpleclouds.config.preset.optimal_mesh", "Optimal Mesh");
		this.add("simpleclouds.config.preset.optimal_mesh.description", "A more complete cloud mesh that fairs better with storm fog. Removes the small delay for chunks to appear that can be seen when using frustum culling, however at the cost of more vertices. Clouds may appear to move with a slight stutter when moving fast.");
		this.add("simpleclouds.config.preset.fast_culled_mesh", "Fast Culled Mesh");
		this.add("simpleclouds.config.preset.fast_culled_mesh.description", "Heavily lowers the total vertex count by applying culling. Generates the cloud mesh much faster. A small delay for chunks to appear is present when turning fast.");
		this.add("gui.simpleclouds.noise_settings.param.range", "Range: %s - %s");
		this.add("gui.simpleclouds.cloud_previewer.button.previous_layer.title", "Previous layer");
		this.add("gui.simpleclouds.cloud_previewer.button.next_layer.title", "Next layer");
		this.add("gui.simpleclouds.cloud_previewer.warning.too_many_cubes", "Warning: Too many cubes");
		this.add("gui.simpleclouds.cloud_previewer.weather_type.title", "Weather Type");
		this.add("gui.simpleclouds.cloud_previewer.storminess.title", "Storminess");
		this.add("gui.simpleclouds.cloud_previewer.storm_start.title", "Storm Start Level");
		this.add("gui.simpleclouds.cloud_previewer.storm_fade_distance.title", "Storm Fade Distance");
		this.add("gui.simpleclouds.cloud_previewer.load.title", "Load");
		this.add("gui.simpleclouds.cloud_previewer.export.title", "Export");
		this.add("gui.simpleclouds.cloud_previewer.popup.select.cloud_type", "Select a cloud type:");
		this.add("gui.simpleclouds.cloud_previewer.popup.export.cloud_type", "What would you like to name your cloud type?");
		this.add("gui.simpleclouds.cloud_previewer.popup.export.exists", "A file with that name already exists. Would you like to override it?");
		this.add("gui.simpleclouds.cloud_previewer.popup.exported.cloud_type", "Your cloud type has been exported to %s");
		this.add("gui.simpleclouds.cloud_previewer.info", "Welcome to the cloud previewer!\n\nAdd, remove, and customize noise layers seen in the left of the screen to create custom cloud types. Use the load button in the bottom right to load existing cloud types to edit them, and use the export button to export your cloud types as JSON files.");
		this.add("gui.simpleclouds.requires_reload.info", "A config option was modified that requires the cloud renderer to be reloaded. Would you like to reload the renderer to apply the changes?");
		this.add("gui.simpleclouds.unknown_or_invalid_client_side_cloud_type.info", "Unknown or invalid cloud type '%s'. Please pick a valid cloud type. \n\nValid cloud types are as follows:\n\n%s");
		this.add("gui.simpleclouds.reload_confirmation.server.info", "A config option was modified on the server that requires the cloud renderer to be reloaded. Please press 'Continue' to continue.");
		this.add("command.simpleclouds.scroll.get", "The current cloud scroll position is [x: %s, y: %s, z: %s]");
		this.add("command.simpleclouds.speed.get", "The current cloud speed is %s");
		this.add("command.simpleclouds.seed.get", "The current cloud seed is %s");
		this.add("command.simpleclouds.reinitialize", "Clouds have been reset");
		this.add("command.simpleclouds.direction.get", "Clouds are currently moving in direction [x: %s, y: %s, z: %s] (%s)");
		this.add("command.simpleclouds.direction.set", "Successfully set cloud direction to [x: %s, y: %s, z: %s] (%s)");
		this.add("command.simpleclouds.height.get", "Cloud height is set to %s");
		this.add("command.simpleclouds.height.set", "Set cloud height to %s");
		this.add("commands.simpleclouds.notClientSideOnly", "Client cloud commands can only be used when connected to servers that do not have Simple Clouds installed. If you're connected on singleplayer, or you are an operator on a dedicated server with Simple Clouds installed, please use '/simpleclouds clouds'");
		this.add("commands.simpleclouds.client.configReferal", "This option is overriden by the CLIENT config. Please refer to the CLIENT config to change this option.");
		this.add("command.simpleclouds.weather.override", "Simple Clouds is overriding vanilla weather, and the /weather command is disabled. To use vanilla weather, please do either of the following:\n1. Set the cloud mode in the SERVER config to AMBIENT.\n2. Set the cloud mode in the SERVER config to SINGLE, and set the single mode cloud type to a cloud type that has no weather associated with it (e.x. simpleclouds:itty_bitty)");
		this.add("gui.simpleclouds.debug.title", "Simple Clouds Debug");
		this.add("simpleclouds.subtitle.distant_thunder", "Distant Thunder Roars");
		this.add("simpleclouds.subtitle.close_thunder", "Thunder Roars");
		this.add("gui.simpleclouds.error_screen.title", "Simple Clouds Error");
		this.add("gui.simpleclouds.error_screen.description", "An error occured while initializing the cloud mesh generator.");
		this.add("gui.simpleclouds.error_screen.no_errors", "There are no errors? What?");
		this.add("gui.simpleclouds.error.recommendations", "Please try updating your graphics drivers. If the issue persists, please make a bug report on the Simple Clouds repository, linked below. Make sure to include the crash report with your issue.\n\nPlease note that Simple Clouds only supports GPUs that support OpenGL 4.3+. If you've updated your graphics drivers and this issue still persists, it is likely your graphics card is too old to support Simple Clouds.");
		this.add("gui.simpleclouds.error.unknown", "Please make a bug report on the mod's GitHub repository, linked below. Make sure to include the crash report and latest.log file with your issue.");
		this.add("gui.simpleclouds.error.couldNotLoadMeshScript", "Failed to load the mesh compute shader. Please make a bug report on the mod's GitHub repository, linked below. Make sure to include the crash report and latest.log file with your issue.\n\nTo developers: If you are modifying the cube_mesh.comp file using a resource pack and have made an error, this message will appear on start up. Please see the latest.log for more details.");
		this.add("gui.simpleclouds.error_screen.button.crash_report", "Crash Report");
	}
}
