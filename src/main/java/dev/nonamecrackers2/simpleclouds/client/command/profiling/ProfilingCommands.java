package dev.nonamecrackers2.simpleclouds.client.command.profiling;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.core.util.ObjectArrayIterator;

import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.cloud.spawning.ClientSideCloudSpawningManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.profiling.ProfilingCloudGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import nonamecrackers2.crackerslib.client.gui.Popup;

public class ProfilingCommands
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal(SimpleCloudsMod.MODID).then(Commands.literal("profiling").requires(stack -> true)
				.then(Commands.literal("generator")
						.then(Commands.argument("time", TimeArgument.time(1))
								.executes(ctx -> runGeneratorProfiler(ctx, IntegerArgumentType.getInteger(ctx, "time")))
						)
						.executes(ctx -> runGeneratorProfiler(ctx, 1728000))
				)
		));
	}

	private static int runGeneratorProfiler(CommandContext<CommandSourceStack> ctx, int iterations) throws CommandSyntaxException
	{
		Popup.createYesNoPopup(null, () ->
		{
			Popup primary = Popup.createInfoPopup(null, 300, Component.literal("Running profiler..."));

			Minecraft mc = Minecraft.getInstance();

			ClientSideCloudTypeManager manager = ClientSideCloudTypeManager.getInstance();
			CloudType[] cloudTypes = manager.getIndexedCloudTypes();
			CloudType[] cachedTypes = Arrays.copyOf(cloudTypes, cloudTypes.length);
			Map<ResourceLocation, CloudType> cachedCloudMap = ImmutableMap.copyOf(manager.getCloudTypes());

			CloudTypeSource wrapper = new CloudTypeSource()
			{
				@Override
				public CloudType[] getIndexedCloudTypes()
				{
					return cachedTypes;
				}

				@Override
				public CloudType getCloudTypeForId(ResourceLocation id)
				{
					return cachedCloudMap.get(id);
				}
			};

			ProfilingCloudGenerator.profile(ClientSideCloudSpawningManager.getClientInstance().getConfig(), wrapper, iterations).exceptionallyAsync(e ->
			{
				ProfilingCloudGenerator.LOGGER.error("Failed to run profiler", e);
				primary.onClose();
				Popup.createInfoPopup(null, 200, Component.literal("Profiler failed. Please see log for details.\n\n" + e.getMessage()));
				return null;
			}, mc).thenAcceptAsync(results ->
			{
				if (results != null)
				{
					primary.onClose();
					try
					{
						acceptResults(results);
					}
					catch (Exception e)
					{
						Popup.createInfoPopup(null, 200, Component.literal("An unknown error occured. See log for more details.\n\n" + e.getMessage()));
						ProfilingCloudGenerator.LOGGER.error("Error when handling results", e);
					}
				}
			}, mc);
		}, 200, Component.literal("You are about to run the cloud generator profiler. This may take a moment. Do you wish to continue?"));

		return 0;
	}

	private static void acceptResults(ProfilingCloudGenerator.Results results)
	{
		MutableComponent mainMessage = Component.literal("Profiler completed. Below is a list of cloud types that spawned. Select a cloud type to see its individual stats.");
		int tickCountElapsed = results.getTotalTicksElapsed();
		mainMessage.append("\n\n");
		mainMessage.append(Component.literal("Total time elapsed: " + humanReadableTicks(tickCountElapsed) + " (" + tickCountElapsed + " ticks)"));
		mainMessage.append("\n");
		mainMessage.append(Component.literal("Total clouds spawned: " + results.getTotalCloudTypesGenerated()));
		int averageSpawnTime = Math.round(results.getAverageSpawnTime());
		mainMessage.append("\n");
		mainMessage.append(Component.literal("Average spawn time: " + humanReadableTicks(averageSpawnTime) + " (" + averageSpawnTime + " ticks)"));
		int averageRainSpawnTime = Math.round(results.getAverageRainSpawnTime());
		mainMessage.append("\n");
		mainMessage.append(Component.literal("Average rain spawn time: " + humanReadableTicks(averageRainSpawnTime) + " (" + averageRainSpawnTime + " ticks)"));
		int averageThunderstormSpawnTime = Math.round(results.getAverageThunderstormSpawnTime());
		mainMessage.append("\n");
		mainMessage.append(Component.literal("Average thunderstorm spawn time: " + humanReadableTicks(averageThunderstormSpawnTime) + " (" + averageThunderstormSpawnTime + " ticks)"));
		mainMessage.append("\n");
		mainMessage.append(createMinMaxInfo("Clouds existing at once", results.getCurrentCloudCountStats()));
		MutableObject<Popup> main = new MutableObject<>();
		Map<ResourceLocation, ProfilingCloudGenerator.CloudStats> individualStats = results.getIndividualStats();
		Consumer<ResourceLocation> valueAcceptor = id -> {
			Popup.createInfoPopup(main.getValue(), 300, createIndividualResults(id, individualStats.get(id))).alignLeft();
		};
		main.setValue(Popup.createOptionListPopup(null, builder -> {
			for (ResourceLocation id : individualStats.keySet())
				builder.addObject(Component.literal(id.toString()), id);
		}, valueAcceptor, 300, 100, mainMessage).alignLeft());
	}
	
	private static Component createIndividualResults(ResourceLocation id, ProfilingCloudGenerator.CloudStats stats)
	{
		MutableComponent message = Component.literal(id.toString());
		message.append("\n\nTotal spawned: " + stats.getTotalSpawned());
		int averageSpawnTicks = Math.round(stats.getAverageTicksToSpawn());
		message.append("\n\nAverage ticks to spawn: " + humanReadableTicks(averageSpawnTicks) + " (" + averageSpawnTicks + " ticks)");
		message.append("\n");
		message.append(createMinMaxTimeInfo("Time over player", stats.getTimeOverPlayer()));
		message.append("\n");
		message.append(createMinMaxInfo("Speed", stats.getSpeedStats()));
		message.append("\n");
		message.append(createMinMaxInfo("Radius", stats.getRadiusStats()));
		message.append("\n");
		message.append(createMinMaxInfo("Stretch factor", stats.getStretchFactorStats()));
		message.append("\n");
		message.append(createMinMaxTimeInfo("Exist time", stats.getExistTicks()));
		message.append("\n");
		message.append(createMinMaxTimeInfo("Grow time", stats.getGrowTicks()));
		return message.withStyle(ChatFormatting.YELLOW);
	}
	
	private static Component createMinMaxTimeInfo(String title, ProfilingCloudGenerator.MinMax minMax)
	{
		String str = String.format("%s; min: %s, max: %s, avg: %s", title, humanReadableTicks(minMax.getMin()), humanReadableTicks(minMax.getMax()), humanReadableTicks(minMax.getAvg()));
		return Component.literal(str);
	}
	
	private static Component createMinMaxInfo(String title, ProfilingCloudGenerator.MinMax minMax)
	{
		String str = String.format("%s; min: %.2f, max: %.2f, avg: %.3f", title, minMax.getMin(), minMax.getMax(), minMax.getAvg());
		return Component.literal(str);
	}

	private static String humanReadableTicks(float ticks)
	{
		Iterator<Pair<Character, Float>> units = new ObjectArrayIterator<>(Pair.of('s', 20.0F), Pair.of('m', 60.0F), Pair.of('h', 60.0F), Pair.of('d', 24.0F));
		char prevUnit;
		Pair<Character, Float> current = Pair.of('t', 1.0F);
		do
		{
			ticks /= current.getRight();
			prevUnit = current.getLeft();
			if (!units.hasNext())
				break;
			current = units.next();
		}
		while (ticks <= -current.getRight() || ticks >= current.getRight());
		return String.format("%.1f%c", ticks, prevUnit);
	}
}
