package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class DevelopmentConfig {
    @SerialEntry
    var apiKey: String = ""

    @SerialEntry
    var overheadStatsRefreshRate: Int = 5
}