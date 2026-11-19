package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch

class PayConfirmConfig {
    @Switch(title = "Confirm large payments", description = "Ask for a click before big /pay commands go out.")
    var enabled: Boolean = true

    @Number(title = "Confirm from amount", description = "Payments of at least this much need confirmation.", min = 0f, max = 1_000_000_000f)
    var confirmThreshold: Long = 5_000_000L

    @Slider(title = "Confirmation timeout", description = "How long a pending payment stays confirmable (seconds).", min = 10f, max = 300f, step = 5f)
    var timeoutSeconds: Int = 60

    init {
        live = this
    }

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: PayConfirmConfig = PayConfirmConfig()
            private set
    }
}
