package dev.nonamecrackers2.simpleclouds.client.framebuffer.compat;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;

public class WrappedRenderTarget extends RenderTarget
{
	private final Supplier<RenderTarget> getter;
	private @Nullable RenderTarget wrapped;
	
	private WrappedRenderTarget(@Nonnull RenderTarget initial, Supplier<RenderTarget> getter)
	{
		super(initial.useDepth);
		this.getter = getter;
		this.setWrapped(initial);
	}
	
	public static WrappedRenderTarget wrapped(Supplier<RenderTarget> getter)
	{
		RenderTarget wrapped = getter.get();
		return new WrappedRenderTarget(wrapped, getter);
	}
	
	@Override
	public void destroyBuffers()
	{
		throw new UnsupportedOperationException("Cannot destroy wrapped render target"); 
	}
	
	@Override
	public void createBuffers(int width, int height, boolean clearErrors)
	{
		throw new UnsupportedOperationException("Cannot create buffers for wrapped render target");
	}
	
	@Override
	public void copyDepthFrom(RenderTarget target)
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").copyDepthFrom(target);
	}
	
	@Override
	public void setFilterMode(int mode)
	{
		throw new UnsupportedOperationException("Cannot set filter mode for wrapped render target"); 
	}
	
	@Override
	public void checkStatus()
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").checkStatus();
	}
	
	@Override
	public void bindRead()
	{
		this.clearWrapped();
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").bindRead();
	}
	
	@Override
	public void unbindRead()
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").unbindRead();
		this.clearWrapped();
	}
	
	@Override
	public void bindWrite(boolean resize)
	{
		this.clearWrapped();
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").bindWrite(resize);
	}
	
	@Override
	public void unbindWrite()
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").unbindWrite();
		this.clearWrapped();
	}
	
	@Override
	public void setClearColor(float r, float g, float b, float a)
	{
		throw new UnsupportedOperationException("Cannot set clear color for wrapped render target"); 
	}
	
	@Override
	public void blitToScreen(int width, int height, boolean blend)
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").blitToScreen(width, height, blend);
	}
	
	@Override
	public void clear(boolean clearErrors)
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").clear(clearErrors);
	}
	
	@Override
	public int getColorTextureId()
	{
		this.setWrapped(null);
		return Objects.requireNonNull(this.wrapped, "Wrapped is not set!").getColorTextureId();
	}
	
	@Override
	public int getDepthTextureId()
	{
		this.setWrapped(null);
		return Objects.requireNonNull(this.wrapped, "Wrapped is not set!").getDepthTextureId();
	}
	
	@Override
	public void resize(int width, int height, boolean clearErrors)
	{
		this.setWrapped(null);
		Objects.requireNonNull(this.wrapped, "Wrapped is not set!").resize(width, height, clearErrors);
	}
	
	@Override
	public boolean isStencilEnabled()
	{
		this.setWrapped(null);
		return Objects.requireNonNull(this.wrapped, "Wrapped is not set!").isStencilEnabled();
	}
	
	@Override
	public void enableStencil()
	{
		throw new UnsupportedOperationException("Cannot set stencil for wrapped render target");
	}
	
	private void setWrapped(@Nullable RenderTarget initial)
	{
		if (this.wrapped != null)
			return;
		if (initial != null)
			this.wrapped = initial;
		else
			this.wrapped = this.getter.get();
		this.width = this.wrapped.width;
		this.height = this.wrapped.height;
		this.viewWidth = this.wrapped.viewWidth;
		this.viewHeight = this.wrapped.viewHeight;
		this.frameBufferId = this.wrapped.frameBufferId;
		this.filterMode = this.wrapped.filterMode;
	}
	
	private void clearWrapped()
	{
		this.wrapped = null;
	}
}
