package dev.nonamecrackers2.simpleclouds.client.vivecraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.Minecraft;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

// Vivecraft doesn't have a seperate API lib so we use reflection
// to not require Vivecraft as a dependency in dev
// These shouldn't be called when Vivecraft is not installed
public class SimpleCloudsVivecraftCompatHandler
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsVivecraftCompatHandler");
	private static final String VIVECRAFT_CLIENTDATAHOLDER_CLASSID = "org.vivecraft.client_vr.ClientDataHolderVR";
	private static @Nullable Method clientDataHolderVRGetter;
	private static boolean thrownError;
	
	private static @Nullable Object getClientDataHolderVR()
	{
		if (clientDataHolderVRGetter == null)
		{
			try
			{
				clientDataHolderVRGetter = Class.forName(VIVECRAFT_CLIENTDATAHOLDER_CLASSID).getMethod("getInstance");
			}
			catch (NoSuchMethodException | SecurityException | ClassNotFoundException e)
			{
				if (!thrownError)
				{
					LOGGER.error("Error getting ClientDataHolderVR getInstance method", e);
					thrownError = true;
				}
			}
		}
		
		try
		{
			return clientDataHolderVRGetter.invoke(null);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			if (!thrownError)
			{
				LOGGER.error("Error getting ClientDataHolderVR", e);
				thrownError = true;
			}
		}
		
		return null;
	}
	
	public static RenderTarget getMainFrameBuffer()
	{
		if (CompatHelper.isVrActive())
		{
			var clientDataHolder = getClientDataHolderVR();
			if (clientDataHolder != null)
			{
				try
				{
					var renderer = clientDataHolder.getClass().getField("vrRenderer").get(clientDataHolder);
					if (renderer != null)
						return (RenderTarget)renderer.getClass().getField("framebufferVrRender").get(renderer);
				}
				catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
				{
					if (!thrownError)
					{
						LOGGER.error("Error getting VR framebufer", e);
						thrownError = true;
					}
				}
			}
		}
		return Minecraft.getInstance().getMainRenderTarget();
	}
	
	public static boolean renderThisPass()
	{
		if (CompatHelper.isVrActive())
		{
			var clientDataHolder = getClientDataHolderVR();
			if (clientDataHolder != null)
			{
				try
				{
					var renderPass = clientDataHolder.getClass().getField("currentPass").get(clientDataHolder);
					return !renderPass.toString().equals("SCOPEL") && !renderPass.toString().equals("SCOPER");
				}
				catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
				{
					if (!thrownError)
					{
						LOGGER.error("Error getting renderpass", e);
						thrownError = true;
					}
				}
			}
		}
		return true;
	}
	
	public static boolean isPrimaryPass()
	{
		if (CompatHelper.isVrActive())
		{
			var clientDataHolder = getClientDataHolderVR();
			if (clientDataHolder != null)
			{
				try
				{
					var renderPass = clientDataHolder.getClass().getField("currentPass").get(clientDataHolder);
					return renderPass.toString().equals("LEFT");
				}
				catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
				{
					if (!thrownError)
					{
						LOGGER.error("Error getting renderpass", e);
						thrownError = true;
					}
				}
			}
		}
		return true;
	}
	
	public static int getStormFogResolutionDivisor()
	{
		if (CompatHelper.isVrActive())
			return 8;
		else
			return 4;
	}
}
