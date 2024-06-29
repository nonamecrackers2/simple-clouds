package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;

public class ModifiableLayeredNoise extends AbstractLayeredNoise<ModifiableNoiseSettings>
{
	public static final Codec<ModifiableLayeredNoise> CODEC = new ListCodec<>(ModifiableNoiseSettings.CODEC).xmap(list -> {
		ModifiableLayeredNoise layered = new ModifiableLayeredNoise();
		for (ModifiableNoiseSettings settings : list)
			layered.addNoiseLayer(settings);
		return layered;
	}, ModifiableLayeredNoise::getNoiseLayers);
	
	public ModifiableLayeredNoise()
	{
		super(Lists.newArrayList());
	}
	
	public List<ModifiableNoiseSettings> getNoiseLayers()
	{
		return this.noiseLayers;
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
