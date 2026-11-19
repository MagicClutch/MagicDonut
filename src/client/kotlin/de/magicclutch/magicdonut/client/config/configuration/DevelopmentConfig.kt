package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Text

class DevelopmentConfig {
    @Text(title = "API key", description = "Your personal DonutSMP API key. Never share it. Leave empty when using a proxy.")
    var apiKey: String = ""

    // Base URL of the stats API. Only change this to point at a proxy you trust.
    @Text(title = "API base URL", description = "Only change this to a proxy you trust.")
    var apiBaseUrl: String = DEFAULT_API_BASE_URL

    init {
        live = this
    }

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.donutsmp.net/v1"

        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: DevelopmentConfig = DevelopmentConfig()
            private set
    }
}
