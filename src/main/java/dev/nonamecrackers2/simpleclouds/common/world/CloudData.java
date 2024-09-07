package dev.nonamecrackers2.simpleclouds.common.world;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import nonamecrackers2.crackerslib.common.util.primitives.PrimitiveHelper;

public class CloudData extends SavedData
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String ID = "clouddata";
	private final CloudManager manager;
	
	public CloudData(CloudManager manager)
	{
		this.manager = manager;
	}
	
	public static CloudData load(CloudManager manager, CompoundTag tag)
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
//		manager.setCloudMode(PrimitiveHelper.readEnum(CloudMode.class, tag, "CloudMode"));
//		CompoundTag singleMode = tag.getCompound("SingleMode");
//		manager.setSingleModeFadeStart(singleMode.getFloat("FadeStart"));
//		manager.setSingleModeFadeEnd(singleMode.getFloat("FadeEnd"));
//		String raw = singleMode.getString("CloudType");
//		ResourceLocation id = ResourceLocation.tryParse(raw);
//		if (id != null)
//			manager.setSingleModeCloudType(id);
//		else
//			LOGGER.warn("Failed to parse id '{}'", raw);
		if (tag.contains("Height"))
			manager.setCloudHeight(tag.getInt("Height"));
		return data;
	}
	
	@Override
	public CompoundTag save(CompoundTag tag)
	{
		tag.putLong("Seed", this.manager.getSeed());
		CompoundTag scroll = new CompoundTag();
		scroll.putFloat("x", this.manager.getScrollX());
		scroll.putFloat("y", this.manager.getScrollY());
		scroll.putFloat("z", this.manager.getScrollZ());
		tag.put("Scroll", scroll);
		tag.put("Direction", PrimitiveHelper.vector3fToTag(this.manager.getDirection()));
		tag.putFloat("Speed", this.manager.getSpeed());
//		PrimitiveHelper.saveEnum(this.manager.getCloudMode(), tag, "CloudMode");
//		CompoundTag singleMode = new CompoundTag();
//		singleMode.putFloat("FadeStart", this.manager.getSingleModeFadeStart());
//		singleMode.putFloat("FadeEnd", this.manager.getSingleModeFadeEnd());
//		singleMode.putString("CloudType", this.manager.getSingleModeCloudType().toString());
//		tag.put("SingleMode", singleMode);
		tag.putInt("Height", this.manager.getCloudHeight());
		return tag;
	}
	
	@Override
	public boolean isDirty()
	{
		return true;
	}
}
