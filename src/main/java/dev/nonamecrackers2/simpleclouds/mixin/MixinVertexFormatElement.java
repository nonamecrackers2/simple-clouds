package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.VertexFormatElement;

@Mixin(VertexFormatElement.class)
public interface MixinVertexFormatElement
{
	@Accessor("BY_ID")
	public static VertexFormatElement[] simpleclouds$getByID()
	{
		throw new AssertionError();
	}
}
