package de.magicclutch.magicdonut.client.config

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Removes leftovers of removed features from the saved config, before OneConfig reads it. OneConfig shows saved
 * entries it does not know as extra options (under "General"), so old sections would clutter the settings screen.
 */
object ConfigCleanup {
    /** Top-level sections of features that no longer exist. */
    private val removedSections = setOf("paymentToast", "paymentAnimation", "streamer", "gambler")

    /** Single options that no longer exist, as "section.option". */
    private val removedOptions = setOf(
        "auction.showHudPanel", "auction.x", "auction.y",
        "auction.defaultMinimumBid", "auction.tickSounds", "auction.tickVolume", "auction.playBidSound",
        "auction.playEndSound", "auction.enabled", "auction.bobbingItem", "auction.bidEffect", "auction.glowRing",
    )

    /** Saved HUDs of removed HUD types (folder name ends with the HUD id). */
    private val removedHudIds = listOf("magicdonut_payment_toasts", "magicdonut_gambler", "magicdonut_payment_banner")

    fun run() {
        val dir = FabricLoader.getInstance().configDir
        try {
            cleanConfigFile(dir.resolve("magicdonututils"))
        } catch (e: Exception) {
            System.err.println("[MagicDonutUtils] Could not clean up the config file: $e")
        }
        try {
            removeOldHuds(dir.resolve("huds"))
        } catch (e: Exception) {
            System.err.println("[MagicDonutUtils] Could not clean up old HUDs: $e")
        }
    }

    private fun cleanConfigFile(file: Path) {
        if (!Files.isRegularFile(file)) return
        val lines = Files.readAllLines(file)
        val kept = ArrayList<String>(lines.size)
        var section: String? = null
        var skipSection = false
        var skipOptionIndent = -1

        for (line in lines) {
            val indent = line.length - line.trimStart().length
            val trimmed = line.trim()

            // A top-level key ("name:" at column 0) starts a new section; a "}" or "]" at column 0 closes one.
            val topLevel = Regex("""^([A-Za-z0-9_]+):""").find(line)
            if (topLevel != null) {
                section = topLevel.groupValues[1]
                skipSection = section in removedSections
                skipOptionIndent = -1
                if (skipSection) continue
                kept.add(line)
                continue
            }
            if (skipSection) continue

            // Inside a section: drop removed options, including their nested lines.
            if (skipOptionIndent >= 0) {
                if (trimmed.isEmpty() || indent > skipOptionIndent) continue
                skipOptionIndent = -1
            }
            val option = Regex("""^\s+([A-Za-z0-9_]+):""").find(line)?.groupValues?.get(1)
            if (section != null && option != null && "$section.$option" in removedOptions) {
                skipOptionIndent = indent
                continue
            }
            kept.add(line)
        }

        if (kept.size != lines.size) Files.write(file, kept)
    }

    private fun removeOldHuds(dir: Path) {
        if (!Files.isDirectory(dir)) return
        Files.list(dir).use { entries ->
            entries.filter { path -> removedHudIds.any { path.fileName.toString().endsWith(it) } }.forEach { deleteRecursively(it) }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (Files.isDirectory(path)) Files.list(path).use { it.forEach(::deleteRecursively) }
        Files.deleteIfExists(path)
    }
}
