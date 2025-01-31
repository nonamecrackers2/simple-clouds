package dev.nonamecrackers2.simpleclouds.client.dh.event;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderPassEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

import dev.nonamecrackers2.simpleclouds.client.dh.SimpleCloudsDhCompatHandler;

public class SimpleCloudsDhSetupHandler extends DhApiBeforeRenderPassEvent
{
	@Override
	public void beforeRender(DhApiEventParam<DhApiRenderParam> event)
	{
		int id = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		if (id == 0)
			throw new RuntimeException("Received no binded framebuffer, expected DH's framebuffer");
		SimpleCloudsDhCompatHandler._updateDhFramebufferId(id);
	}
}
