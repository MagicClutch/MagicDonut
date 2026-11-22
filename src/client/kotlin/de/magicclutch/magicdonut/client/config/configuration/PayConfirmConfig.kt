package de.magicclutch.magicdonut.client.config.configuration

import com.mojang.blaze3d.platform.InputConstants
import de.magicclutch.magicdonut.client.payconfirm.QuickPayState
import org.polyfrost.oneconfig.api.config.v1.annotations.Keybind
import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import java.util.function.Consumer

class PayConfirmConfig {
    @Switch(title = "Confirm large payments", description = "Ask before a big /pay goes out.")
    var enabled: Boolean = true

    @Number(title = "From amount", description = "Payments of at least this much need confirmation.", min = 0f, max = 1_000_000_000f)
    var confirmThreshold: Long = 5_000_000L

    @Slider(title = "Timeout", description = "Seconds a pending payment stays confirmable.", min = 10f, max = 300f, step = 5f)
    var timeoutSeconds: Int = 60

    @Switch(title = "Popup window", description = "Ask in a Yes/No window instead of chat buttons.")
    var usePopup: Boolean = false

    @Keybind(title = "Quick pay key", description = "Hold it and right-click a player to open /pay for them.")
    var quickPayKey: OneConfigKeybind = KeybindHelper.builder()
        .key(InputConstants.KEY_LCONTROL)
        .name("Quick pay")
        .category("MagicDonutUtils")
        .action(Consumer<Boolean> { pressed -> QuickPayState.held = pressed })
        .build()

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
