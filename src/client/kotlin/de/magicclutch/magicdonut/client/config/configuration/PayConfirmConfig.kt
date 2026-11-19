package de.magicclutch.magicdonut.client.config.configuration


class PayConfirmConfig {
    var enabled: Boolean = true

    var confirmThreshold: Long = 5_000_000L

    var timeoutSeconds: Int = 60

}