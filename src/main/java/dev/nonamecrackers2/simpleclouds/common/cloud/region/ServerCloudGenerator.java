package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class ServerCloudGenerator extends CloudGenerator
{
	private static final int AUTO_SYNC_INTERVAL = 240;
	private int syncTimer = AUTO_SYNC_INTERVAL;
	private boolean requiresSync;
	
	public ServerCloudGenerator(CloudGetter getter, int intialMaximumAmount)
	{
		super(getter, intialMaximumAmount);
	}
	
	public boolean checkAndResetSync()
	{
		boolean flag = this.requiresSync;
		this.requiresSync = false;
		return flag;
	}
	
	@Override
	public boolean addCloud(CloudRegion region)
	{
		if (!super.addCloud(region))
			return false;
		this.requiresSync = true;
		return true;
	}
	
	@Override
	public boolean removeAllClouds()
	{
		if (!super.removeAllClouds())
			return false;
		this.requiresSync = true;
		return true;
	}
	
	@Override
	public void tick(RandomSource random, Level level)
	{
		super.tick(random, level);
		
		if (this.syncTimer > 0)
		{
			this.syncTimer--;
			if (this.syncTimer == 0)
			{
				this.requiresSync = true;
				this.syncTimer = AUTO_SYNC_INTERVAL;
			}
		}
	}

	@Override
	protected void generateCloud(RandomSource random, Level level, boolean initial)
	{
		this.addCloud(new CloudRegion(this.cloudGetter.getIndexedCloudTypes()[1].id(), new Vec2(1.0F, 0.0F), 0.0F, 0.0F, 1220.0F, -823.0F, 500.0F, 120000));
		this.addCloud(new CloudRegion(this.cloudGetter.getIndexedCloudTypes()[2].id(), new Vec2(-1.0F, 0.0F), 0.0F, 0.0F, 820.0F, -723.0F, 400.0F, 120000));
	}
}
