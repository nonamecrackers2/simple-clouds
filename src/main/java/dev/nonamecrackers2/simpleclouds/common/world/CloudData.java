package dev.nonamecrackers2.simpleclouds.common.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import nonamecrackers2.crackerslib.common.util.primitives.PrimitiveHelper;

public class CloudData extends SavedData
{
	public static final String ID = "clouddata";
	private final CloudManager<?> manager;
	
	public CloudData(CloudManager<?> manager)
	{
		this.manager = manager;
	}
	
	public static SavedData.Factory<CloudData> factory(CloudManager<?> manager)
	{
		return new SavedData.Factory<>(() -> new CloudData(manager), (tag, lookup) -> load(manager, tag, lookup));
	}
	
	public static CloudData load(CloudManager<?> manager, CompoundTag tag, HolderLookup.Provider provider)
	{
		CloudData data = new CloudData(manager);
		if (tag.contains("Seed"))
			manager.setSeed(tag.getLong("Seed"));
		if (tag.contains("Scroll", 10))
		{
			CompoundTag scroll = tag.getCompound("Scroll");
			manager.setScrollX(scroll.getFloat("x"));
			manager.setScrollY(scroll.getFloat("y"));
			manager.setScrollZ(scroll.getFloat("z"));
		}
		if (tag.contains("Direction", 10))
			manager.setDirection(PrimitiveHelper.vector3fFromTag(tag.getCompound("Direction")));
		if (tag.contains("Speed"))
			manager.setSpeed(tag.getFloat("Speed"));
		if (tag.contains("Height"))
			manager.setCloudHeight(tag.getInt("Height"));
		return data;
	}
	
	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
	{
		tag.putLong("Seed", this.manager.getSeed());
		CompoundTag scroll = new CompoundTag();
		scroll.putFloat("x", this.manager.getScrollX());
		scroll.putFloat("y", this.manager.getScrollY());
		scroll.putFloat("z", this.manager.getScrollZ());
		tag.put("Scroll", scroll);
		tag.put("Direction", PrimitiveHelper.vector3fToTag(this.manager.getDirection()));
		tag.putFloat("Speed", this.manager.getSpeed());
		tag.putInt("Height", this.manager.getCloudHeight());
		return tag;
	}
	
	@Override
	public boolean isDirty()
	{
		return true;
	}
}
