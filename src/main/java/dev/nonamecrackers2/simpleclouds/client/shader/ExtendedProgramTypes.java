package dev.nonamecrackers2.simpleclouds.client.shader;

import java.util.Objects;

import javax.annotation.Nullable;

import com.mojang.blaze3d.shaders.Program;

public class ExtendedProgramTypes
{
	private static @Nullable Program.Type geometry;
	
	public static void setGeometryType(Program.Type type)
	{
		if (geometry != null)
			throw new IllegalStateException();
		geometry = Objects.requireNonNull(type);
	}
	
	public static Program.Type geometry()
	{
		return Objects.requireNonNull(geometry, "Geometry program type not initialized");
	}
}
