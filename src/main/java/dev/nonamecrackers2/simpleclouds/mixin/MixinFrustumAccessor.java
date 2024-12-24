package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.culling.Frustum;

@Mixin(Frustum.class)
public interface MixinFrustumAccessor
{
	@Invoker("cubeInFrustum")
	boolean simpleclouds$cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
