package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class PayConfirmConfig {
    @SerialEntry
    var enabled: Boolean = true

    @SerialEntry
    var confirmThreshold: Long = 5_000_000L

    @SerialEntry
    var timeoutSeconds: Int = 60

}