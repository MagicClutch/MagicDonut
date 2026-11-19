package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class DevelopmentConfig {
    @SerialEntry
    var apiKey: String = ""

    // Base URL of the stats API. Only change this to point at a proxy you trust.
    @SerialEntry
    var apiBaseUrl: String = DEFAULT_API_BASE_URL

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.donutsmp.net/v1"
    }
}
