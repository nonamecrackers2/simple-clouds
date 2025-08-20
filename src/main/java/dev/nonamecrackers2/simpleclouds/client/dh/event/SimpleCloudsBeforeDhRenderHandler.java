package dev.nonamecrackers2.simpleclouds.client.dh.event;

import org.joml.Matrix4f;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

import dev.nonamecrackers2.simpleclouds.client.dh.SimpleCloudsDhCompatHandler;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class SimpleCloudsBeforeDhRenderHandler extends DhApiBeforeApplyShaderRenderEvent
{
	@Override
	public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event)
	{
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		CloudsRenderPipeline pipeline = renderer.getRenderPipeline();
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		
		SimpleCloudsDhCompatHandler._markPassComplete(false);
		
		DhApiRenderParam params = event.value;
		Matrix4f projMat = new Matrix4f().setTransposed(params.dhProjectionMatrix.getValuesAsArray());
		Matrix4f modelView = new Matrix4f().setTransposed(params.mcModelViewMatrix.getValuesAsArray());
		
		SimpleCloudsDhCompatHandler._updateCachedDhState(projMat, modelView);
		
		int fbo = SimpleCloudsDhCompatHandler._getDhFramebufferId();
		
		if (SimpleCloudsRenderer.canRenderInDimension(mc.level))
			pipeline.beforeDistantHorizonsApplyShader(mc, renderer, modelView, projMat, params.partialTicks, camPos.x, camPos.y, camPos.z, renderer.getCullFrustum(), fbo);
	}
}
