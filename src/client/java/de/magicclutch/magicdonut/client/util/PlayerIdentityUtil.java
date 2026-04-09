package de.magicclutch.magicdonut.client.util;

import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PlayerIdentityUtil {
    // Match legacy Minecraft formatting codes, including the common "Â§" mojibake variant.
    private static final Pattern FORMATTING_CODE =
        Pattern.compile("(?:\u00C2\u00A7|\u00A7)[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_FORMATTING_CODE =
        Pattern.compile("(?:\u00C2\u00A7|\u00A7)x(?:(?:\u00C2\u00A7|\u00A7)[0-9a-f]){6}", Pattern.CASE_INSENSITIVE);

    // Common rank prefix pattern (e.g. "[VIP] Name").
    private static final Pattern BRACKET_PREFIX = Pattern.compile("^\\[[^\\]]+\\]\\s*");

    private PlayerIdentityUtil() {
    }


    public static String getCanonicalUsername(Entity entity) {
        if (entity == null) return null;

        // If available, prefer the underlying profile name over display names.
        if (entity instanceof Player player) {
            String normalized = normalizeUsernameForApi(player.getGameProfile().name());
            if (normalized != null) return normalized;
        }

        // Prefer base name over display name to avoid team prefix/suffix.
        String normalized = normalizeUsernameForApi(componentToString(entity.getName()));
        if (normalized != null) return normalized;

        // Fallback: display name may include ranks/prefixes/suffixes, but sanitize hard anyway.
        normalized = normalizeUsernameForApi(componentToString(entity.getDisplayName()));
        if (normalized != null) return normalized;

        return null;
    }

    public static String normalizeUsernameForApi(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Remove formatting codes first, then common rank prefix like "[VIP] ".
        s = HEX_FORMATTING_CODE.matcher(s).replaceAll("");
        s = FORMATTING_CODE.matcher(s).replaceAll("");
        s = BRACKET_PREFIX.matcher(s).replaceFirst("").trim();

        // Hard-allowlist: only letters, digits, '_' and '.'.
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isAllowedApiUsernameChar(c)) out.append(c);
        }

        // Strip leading dots (treat as prefixes; keep dots inside the name).
        int start = 0;
        while (start < out.length() && out.charAt(start) == '.') start++;

        String cleaned = out.substring(start);
        if (cleaned.isBlank()) return null;

        return cleaned.toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedApiUsernameChar(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '_'
            || c == '.';
    }

    private static String componentToString(Component component) {
        return component == null ? null : component.getString();
    }
}
