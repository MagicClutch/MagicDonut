package de.magicclutch.magicdonut.client.mixin;

import de.magicclutch.magicdonut.client.overheadstats.CustomStatRenderState;
import de.magicclutch.magicdonut.client.util.PlayerIdentityUtil;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererUpdateMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void magicdonut$setCanonicalUsername(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        CustomStatRenderState customState = (CustomStatRenderState) state;
        customState.magicdonut$setCanonicalUsername(PlayerIdentityUtil.getCanonicalUsername(entity));
        customState.magicdonut$setDrawn(false);
    }
}