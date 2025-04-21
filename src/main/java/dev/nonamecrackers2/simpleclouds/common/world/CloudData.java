package dev.nonamecrackers2.simpleclouds.common.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class CloudData extends SavedData
{
	public static final String ID = "clouddata";
	private final ServerCloudManager manager;
	
	public CloudData(ServerCloudManager manager)
	{
		this.manager = manager;
	}
	
	public static CloudData load(ServerCloudManager manager, CompoundTag tag)
	{
		CloudData data = new CloudData(manager);
		if (tag.contains("Seed"))
			manager.setSeed(tag.getLong("Seed"));
		if (tag.contains("ScrollAngle"))
			manager.setScrollAngle(tag.getFloat("ScrollAngle"));
		if (tag.contains("Speed"))
			manager.setSpeed(tag.getFloat("Speed"));
		if (tag.contains("Height"))
			manager.setCloudHeight(tag.getInt("Height"));
		manager.getCloudGenerator().readTag(tag.getCompound("cloud_generator"));
		return data;
	}
	
	@Override
	public CompoundTag save(CompoundTag tag)
	{
		tag.putLong("Seed", this.manager.getSeed());
		tag.putFloat("ScrollAngle", this.manager.getScrollAngle());
		tag.putFloat("Speed", this.manager.getSpeed());
		tag.putInt("Height", this.manager.getCloudHeight());
		tag.put("cloud_generator", this.manager.getCloudGenerator().toTag());
		return tag;
	}
	
	@Override
	public boolean isDirty()
	{
		return true;
	}
}
