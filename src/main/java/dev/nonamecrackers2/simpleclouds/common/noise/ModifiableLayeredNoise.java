package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Collection;

import com.google.common.collect.Lists;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class ModifiableLayeredNoise extends AbstractLayeredNoise<ModifiableNoiseSettings>
{
	public ModifiableLayeredNoise()
	{
		super(Lists.newArrayList());
	}
	
	public ModifiableLayeredNoise(Collection<ModifiableNoiseSettings> layers)
	{
		super(Lists.newArrayList(layers));
	}
	
	public ModifiableLayeredNoise addNoiseLayer(ModifiableNoiseSettings layer)
	{
		if (!this.noiseLayers.contains(layer))
			this.noiseLayers.add(layer);
		return this;
	}
	
	@Override
	public <T> DataResult<T> encode(DynamicOps<T> ops, T prefix)
	{
		return StaticLayeredNoise.CODEC.encode(this.toStatic(), ops, prefix);
	}
	
	public boolean removeNoiseLayer(ModifiableNoiseSettings layer)
	{
		return this.noiseLayers.remove(layer);
	}
	
	public StaticLayeredNoise toStatic()
	{
		return new StaticLayeredNoise(this);
	}
}
