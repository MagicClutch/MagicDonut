package de.magicclutch.magicdonut.client.profit

import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.ScoreHolder
import org.slf4j.LoggerFactory

/** Dumps everything the scoreboard exposes to the log, to find out where the money line lives. */
object ScoreboardDebug {
    private val logger = LoggerFactory.getLogger("MagicDonutUtils/ScoreboardDebug")

    // A number with an optional k/m/b/t suffix, e.g. 1,234.5K or 2.5m
    private val amountPattern = Regex("""\d[\d,]*(?:\.\d+)?\s?[kKmMbBtT]?""")

    /** Logs the scoreboard and returns a one-line summary for chat. */
    fun dump(mc: Minecraft): String {
        val level = mc.level ?: return "Not in a world."
        val scoreboard = level.scoreboard

        logger.info("===== Scoreboard dump =====")
        logger.info("Objectives: {}", scoreboard.objectives.joinToString { "'${it.name}' (${it.displayName.string})" })
        for (slot in DisplaySlot.entries) {
            val objective = scoreboard.getDisplayObjective(slot)
            logger.info("Display slot {}: {}", slot, objective?.name ?: "-")
        }

        logger.info("Tab list footer: raw='{}' parsedBalance={}", TabListFooter.text, TabListFooter.balance)
        dumpTabList(mc)

        val sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)
        if (sidebar == null) {
            logger.info("No sidebar objective. Nothing to read.")
            return "No sidebar scoreboard found (see log)."
        }
        logger.info("Sidebar objective: '{}' title='{}'", sidebar.name, sidebar.displayName.string)

        var found = 0
        for (entry in scoreboard.listPlayerScores(sidebar)) {
            if (entry.isHidden) continue
            val team = scoreboard.getPlayersTeam(entry.owner())
            val line = PlayerTeam.formatNameForTeam(team, entry.ownerName()).string
            val custom = (entry.display() as Component?)?.string
            logger.info(
                "line owner='{}' score={} ownerName='{}' customDisplay='{}' teamPrefix='{}' teamSuffix='{}' => TEXT='{}'",
                entry.owner(), entry.value(), entry.ownerName().string, custom,
                team?.playerPrefix?.string, team?.playerSuffix?.string, line
            )
            val amounts = amountPattern.findAll(line).map { it.value.trim() }.toList()
            if (amounts.isNotEmpty()) {
                logger.info("    numbers in line: {}", amounts.joinToString { "'$it' -> ${AmountParser.parse(it.replace(",", ""))}" })
            }
            found++
        }
        logger.info("===== End of dump ({} sidebar lines) =====", found)
        return "Scoreboard dumped: $found sidebar lines. Check the log/console."
    }

    /** Reads the tab list data without opening it: your own entry in full, then a few others for comparison. */
    private fun dumpTabList(mc: Minecraft) {
        val level = mc.level ?: return
        val connection = mc.connection ?: return
        val scoreboard = level.scoreboard
        val listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST)
        logger.info("--- Tab list --- (LIST objective: {})", listObjective?.name ?: "none")

        val own = mc.player?.name?.string
        val infos = connection.onlinePlayers.sortedByDescending { it.profile.name == own }
        for (info in infos.take(6)) {
            val name = info.profile.name
            val team = scoreboard.getPlayersTeam(name)
            val teamText = PlayerTeam.formatNameForTeam(team, Component.literal(name)).string
            val score = listObjective?.let { scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(name), it)?.value() }
            logger.info(
                "tab player='{}'{} ping={}ms customTabName='{}' teamFormatted='{}' listScore={}",
                name, if (name == own) " (YOU)" else "", info.latency,
                info.tabListDisplayName?.string, teamText, score
            )
        }
        logger.info("--- End tab list ({} players) ---", infos.size)
    }
}
