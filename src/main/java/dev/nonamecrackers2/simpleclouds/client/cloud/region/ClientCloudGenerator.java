package dev.nonamecrackers2.simpleclouds.client.cloud.region;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class ClientCloudGenerator extends CloudGenerator
{
	public ClientCloudGenerator(CloudGetter cloudGetter, int initialMaximumAmount)
	{
		super(cloudGetter, initialMaximumAmount);
	}

	@Override
	protected void generateCloud(RandomSource random, Level level, boolean initial)
	{
		//this.addCloud(new CloudRegion(this.cloudGetter.getIndexedCloudTypes()[1].id(), new Vec2(1.0F, 0.0F), 0.0F, 0.0F, 1220.0F, -823.0F, 500.0F, 120000));
		//this.addCloud(new CloudRegion(this.cloudGetter.getIndexedCloudTypes()[1].id(), new Vec2(-1.0F, 0.0F), 0.0F, 0.0F, 820.0F, -723.0F, 400.0F, 120000));
	}
}
