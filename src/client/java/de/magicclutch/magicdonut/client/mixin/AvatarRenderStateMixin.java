package de.magicclutch.magicdonut.client.mixin;

import de.magicclutch.magicdonut.client.overheadstats.CustomStatRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements CustomStatRenderState {
    @Unique
    private String magicdonut$statText;
    @Unique
    private int magicdonut$statColor;
    @Unique
    private String magicdonut$canonicalUsername;
    @Unique
    private boolean magicdonut$drawn;

    @Override
    public String magicdonut$getStatText() {
        return this.magicdonut$statText;
    }

    @Override
    public void magicdonut$setStatText(String text) {
        this.magicdonut$statText = text;
    }

    @Override
    public int magicdonut$getStatColor() {
        return this.magicdonut$statColor;
    }

    @Override
    public void magicdonut$setStatColor(int color) {
        this.magicdonut$statColor = color;
    }

    @Override
    public boolean magicdonut$hasStatText() {
        return this.magicdonut$statText != null && !this.magicdonut$statText.isBlank();
    }

    @Override
    public void magicdonut$clearDisplayData() {
        this.magicdonut$statText = null;
        this.magicdonut$statColor = 0;
        this.magicdonut$drawn = false;
    }

    @Override
    public String magicdonut$getCanonicalUsername() {
        return this.magicdonut$canonicalUsername;
    }

    @Override
    public void magicdonut$setCanonicalUsername(String username) {
        this.magicdonut$canonicalUsername = username;
    }

    @Override
    public boolean magicdonut$hasDrawn() {
        return this.magicdonut$drawn;
    }

    @Override
    public void magicdonut$setDrawn(boolean drawn) {
        this.magicdonut$drawn = drawn;
    }
}