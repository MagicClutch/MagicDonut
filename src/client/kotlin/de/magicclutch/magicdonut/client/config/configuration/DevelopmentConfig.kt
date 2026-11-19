package de.magicclutch.magicdonut.client.config.configuration


class DevelopmentConfig {
    var apiKey: String = ""

    // Base URL of the stats API. Only change this to point at a proxy you trust.
    var apiBaseUrl: String = DEFAULT_API_BASE_URL

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.donutsmp.net/v1"
    }
}
