package de.magicclutch.magicdonut.client.util;

import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerIdentityUtil {
    private static final Pattern FORMATTING_CODE = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRACKET_PREFIX = Pattern.compile("^\\[[^\\]]+\\]\\s*");

    private PlayerIdentityUtil() {
    }

    public static String getCanonicalUsername(Entity entity) {
        if (entity == null) {
            return null;
        }

        Component displayName = entity.getDisplayName();
        if (displayName != null) {
            String stripped = FORMATTING_CODE.matcher(displayName.getString()).replaceAll("");
            stripped = BRACKET_PREFIX.matcher(stripped).replaceFirst("").trim();
            if (!stripped.isBlank()) {
                return stripped.toLowerCase(Locale.ROOT);
            }
        }

        if (entity instanceof Player player) {
            return player.getGameProfile().name().toLowerCase(Locale.ROOT);
        }

        return null;
    }
}