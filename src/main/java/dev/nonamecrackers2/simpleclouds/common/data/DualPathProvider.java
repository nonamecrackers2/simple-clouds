package dev.nonamecrackers2.simpleclouds.common.data;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public abstract class DualPathProvider implements DataProvider
{
	protected final List<PackOutput.PathProvider> paths;
	
	public DualPathProvider(PackOutput output, String loc)
	{
		this.paths = ImmutableList.of(
				output.createPathProvider(PackOutput.Target.RESOURCE_PACK, loc),
				output.createPathProvider(PackOutput.Target.DATA_PACK, loc)
		);
	}
	
	protected final void jsonForPaths(ResourceLocation id, Consumer<Path> consumer)
	{
		this.paths.forEach(p -> consumer.accept(p.json(id)));
	}
}
