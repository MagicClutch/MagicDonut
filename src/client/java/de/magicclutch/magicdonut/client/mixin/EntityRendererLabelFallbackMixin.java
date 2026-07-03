package de.magicclutch.magicdonut.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.magicclutch.magicdonut.client.overheadstats.CustomStatRenderState;
import de.magicclutch.magicdonut.client.overheadstats.OverheadStatNameTagRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererLabelFallbackMixin {
    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void magicdonut$renderOverheadStatsFallback(EntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatarState && state instanceof CustomStatRenderState customState && !customState.magicdonut$hasDrawn()) {
            OverheadStatNameTagRenderer.populateAndRender(avatarState, customState, matrices, queue, cameraState);
        }
    }
}
