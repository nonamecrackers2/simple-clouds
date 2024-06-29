package dev.nonamecrackers2.simpleclouds.common.cloud;

import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;

public record CloudType(float storminess, float stormStart, float stormFadeDistance, NoiseSettings noiseConfig) implements CloudInfo
{
}
