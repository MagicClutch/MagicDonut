package de.magicclutch.magicdonut.client.overheadstats;

import com.mojang.blaze3d.vertex.PoseStack;
import de.magicclutch.magicdonut.client.config.MagicDonutConfig;
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig;
import de.magicclutch.magicdonut.client.overheadstats.StatsRenderer.RenderLabel;
import de.magicclutch.magicdonut.client.util.PlayerIdentityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class OverheadStatNameTagRenderer {
    private static final FontDescription ICON_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("magicdonut", "icons"));
    private static final String AMETHYST_SHARD_GLYPH = "\uE101";
    private static final String KILLS_GLYPH = "\uE102";
    private static final String PLAYTIME_GLYPH = "\uE103";
    private static final int DEFAULT_LIGHT = 15728880;

    private OverheadStatNameTagRenderer() {
    }

    public static void populateAndRender(EntityRenderState state, CustomStatRenderState customState, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        Vec3 nameLabelPos = state.nameTagAttachment;
        if (nameLabelPos == null || !MagicDonutConfig.Companion.getInstance().getOverheadStatsConfig().getEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        String canonicalUsername = customState.magicdonut$getCanonicalUsername();
        if (canonicalUsername == null || canonicalUsername.isBlank()) {
            return;
        }

        String selfUsername = PlayerIdentityUtil.getCanonicalUsername(client.player);
        if (canonicalUsername.equalsIgnoreCase(selfUsername)) {
            return;
        }

        customState.magicdonut$clearDisplayData();

        RenderLabel label = StatsRenderer.INSTANCE.getRenderLabel(canonicalUsername);
        if (label == null) {
            return;
        }

        customState.magicdonut$setStatText(label.getText());
        customState.magicdonut$setStatColor(label.getColor());
        drawLabel(state, label.getStat(), customState, matrices, queue, cameraState, nameLabelPos);
        customState.magicdonut$setDrawn(true);
    }

    private static void drawLabel(EntityRenderState state, OverheadStatsConfig.StatField stat, CustomStatRenderState customState, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, Vec3 nameLabelPos) {
        int light = state.lightCoords == 0 ? DEFAULT_LIGHT : state.lightCoords;
        boolean notSneaking = !state.isDiscrete;

        float yOffset = MagicDonutConfig.Companion.getInstance().getOverheadStatsConfig().getStatYOffset();
        float scale = MagicDonutConfig.Companion.getInstance().getOverheadStatsConfig().getStatScale();

        Vec3 statPos = new Vec3(nameLabelPos.x, nameLabelPos.y + yOffset, nameLabelPos.z);

        Component statText;
        if (stat == OverheadStatsConfig.StatField.SHARDS) {
            Component icon = Component.literal(AMETHYST_SHARD_GLYPH).withStyle(style -> style.withFont(ICON_FONT));
            Component value = Component.literal(customState.magicdonut$getStatText())
                .withStyle(style -> style.withColor(customState.magicdonut$getStatColor()));
            statText = Component.empty().append(icon).append(" ").append(value);
        } else if (stat == OverheadStatsConfig.StatField.KILLS) {
            Component icon = Component.literal(KILLS_GLYPH).withStyle(style -> style.withFont(ICON_FONT));
            Component value = Component.literal(customState.magicdonut$getStatText())
                .withStyle(style -> style.withColor(customState.magicdonut$getStatColor()));
            statText = Component.empty().append(icon).append(" ").append(value);
        } else if (stat == OverheadStatsConfig.StatField.PLAYTIME) {
            Component icon = Component.literal(PLAYTIME_GLYPH).withStyle(style -> style.withFont(ICON_FONT));
            Component value = Component.literal(customState.magicdonut$getStatText())
                .withStyle(style -> style.withColor(customState.magicdonut$getStatColor()));
            statText = Component.empty().append(icon).append(" ").append(value);
        } else {
            statText = Component.literal(customState.magicdonut$getStatText())
                .withStyle(style -> style.withColor(customState.magicdonut$getStatColor()));
        }

        matrices.pushPose();
        matrices.scale(scale, scale, scale);
        queue.submitNameTag(matrices, statPos, 0, statText, notSneaking, light, state.distanceToCameraSq, cameraState);
        matrices.popPose();
    }
}
