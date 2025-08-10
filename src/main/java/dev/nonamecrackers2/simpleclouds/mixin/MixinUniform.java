package dev.nonamecrackers2.simpleclouds.mixin;

import java.nio.FloatBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.shaders.Uniform;

@Mixin(Uniform.class)
public abstract class MixinUniform
{
	//Fixes a bug with using matrix2x2 and matrix3x3 as shader uniforms
	@Redirect(method = "set([F)V", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put([F)Ljava/nio/FloatBuffer;"))
	private FloatBuffer simpleclouds$fixMatrixBug(FloatBuffer buffer, float[] values)
	{
		return buffer.put(values, 0, buffer.limit());
	}
}
