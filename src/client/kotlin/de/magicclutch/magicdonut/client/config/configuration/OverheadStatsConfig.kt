package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class OverheadStatsConfig {
    @SerialEntry
    var enabled: Boolean = true

    @SerialEntry
    var stat: StatField = StatField.MONEY

    enum class StatField(val apiKey: String) {
        BROKEN_BLOCKS("broken_blocks"),
        DEATHS("deaths"),
        KILLS("kills"),
        MOBS_KILLED("mobs_killed"),
        MONEY("money"),
        MONEY_MADE_FROM_SELL("money_made_from_sell"),
        MONEY_SPENT_ON_SHOP("money_spent_on_shop"),
        PLACED_BLOCKS("placed_blocks"),
        PLAYTIME("playtime"),
        SHARDS("shards");
    }
}
