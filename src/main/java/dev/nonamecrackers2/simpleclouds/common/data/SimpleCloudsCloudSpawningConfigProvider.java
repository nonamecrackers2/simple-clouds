package dev.nonamecrackers2.simpleclouds.common.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.util.random.Weight;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;

public class SimpleCloudsCloudSpawningConfigProvider extends CloudSpawningConfigProvider
{
	private static final IntProvider SPAWN_INTERVAL = BiasedToBottomInt.of(120, 360);
	private static final int MAX_FORMATIONS = 5;
	private static final int MAX_INITIAL_FORMATIONS = 3;
	
	public SimpleCloudsCloudSpawningConfigProvider(PackOutput output)
	{
		super(output);
	}

	@Override
	protected void addEntries()
	{
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("cumulonimbus"), 
				Weight.of(1), 
				UniformFloat.of(0.2F, 0.4F),
				BiasedToBottomInt.of(6000, 9000), 
				BiasedToBottomInt.of(24000, 48000), 
				UniformInt.of(1200, 2400), 
				UniformFloat.of(0.2F, 0.4F), 
				true, 
				1000)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("nimbostratus"), 
				Weight.of(2), 
				UniformFloat.of(0.2F, 0.4F),
				BiasedToBottomInt.of(6000, 9000), 
				BiasedToBottomInt.of(24000, 48000), 
				UniformInt.of(1200, 2400), 
				UniformFloat.of(0.2F, 0.4F), 
				true, 
				900)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("stratus"), 
				Weight.of(5), 
				UniformFloat.of(0.2F, 0.4F),
				BiasedToBottomInt.of(6000, 9000), 
				BiasedToBottomInt.of(24000, 48000), 
				UniformInt.of(1200, 2400), 
				UniformFloat.of(0.2F, 0.4F), 
				true, 
				800)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("stratocumulus"), 
				Weight.of(8), 
				UniformFloat.of(0.1F, 0.2F),
				BiasedToBottomInt.of(5000, 8000), 
				BiasedToBottomInt.of(12000, 36000), 
				UniformInt.of(1200, 2400), 
				ConstantFloat.of(1.0F), 
				false, 
				700)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("cumulus"), 
				Weight.of(12), 
				UniformFloat.of(0.1F, 0.2F),
				BiasedToBottomInt.of(4000, 10000), 
				BiasedToBottomInt.of(12000, 48000), 
				UniformInt.of(1200, 2400), 
				ConstantFloat.of(1.0F), 
				false, 
				600)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("small_cumulus"), 
				Weight.of(10), 
				UniformFloat.of(0.1F, 0.2F),
				BiasedToBottomInt.of(4000, 10000), 
				BiasedToBottomInt.of(12000, 48000), 
				UniformInt.of(1200, 2400), 
				ConstantFloat.of(1.0F), 
				false, 
				500)
		);
		this.addEntry(new CloudSpawningConfig.Info(
				SimpleCloudsMod.id("itty_bitty"), 
				Weight.of(12), 
				UniformFloat.of(0.1F, 0.2F),
				BiasedToBottomInt.of(4000, 10000), 
				BiasedToBottomInt.of(12000, 48000), 
				UniformInt.of(1200, 2400), 
				ConstantFloat.of(1.0F), 
				false, 
				400)
		);
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput output)
	{
		JsonObject root = new JsonObject();
		root.add("spawn_interval", IntProvider.NON_NEGATIVE_CODEC.encodeStart(JsonOps.INSTANCE, SPAWN_INTERVAL).resultOrPartial(e -> {
			throw new IllegalArgumentException(e);
		}).get());
		root.addProperty("max_formations", MAX_FORMATIONS);
		root.addProperty("max_initial_formations", MAX_INITIAL_FORMATIONS);
		
		List<CompletableFuture<?>> futures = Lists.newArrayList();
		
		this.jsonForPaths(SimpleCloudsMod.id("config"), p -> {
			futures.add(DataProvider.saveStable(output, root, p));
		});
		
		futures.add(super.run(output));
		return CompletableFuture.allOf(futures.toArray(i -> new CompletableFuture[i]));
	}
}
