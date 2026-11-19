package de.magicclutch.magicdonut.client.mixin;

import de.magicclutch.magicdonut.client.profit.TabListFooter;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the tab list footer (where DonutSMP shows your money and ping) as soon as the server sends it. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "setFooter(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void magicdonut$captureFooter(Component footer, CallbackInfo ci) {
        TabListFooter.INSTANCE.update(footer);
    }
}
