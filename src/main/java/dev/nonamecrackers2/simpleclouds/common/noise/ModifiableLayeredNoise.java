package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Collection;

import com.google.common.collect.Lists;

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
	
	public boolean removeNoiseLayer(ModifiableNoiseSettings layer)
	{
		return this.noiseLayers.remove(layer);
	}
	
	public StaticLayeredNoise toStatic()
	{
		return new StaticLayeredNoise(this);
	}
}
