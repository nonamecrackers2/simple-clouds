package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.common.data.SimpleCloudsLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class SimpleCloudsDataEvents
{
	public static void gatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		generator.addProvider(event.includeClient(), (DataProvider.Factory<SimpleCloudsLangProvider>)SimpleCloudsLangProvider::new);
	}
}
