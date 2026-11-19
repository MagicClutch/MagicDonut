package de.magicclutch.magicdonut.client.config.configuration

/**
 * Settings of the stream gamble overlay: a viewer pays you, an item animation decides who wins.
 * The odds are always shown on screen so viewers know exactly what they play.
 */
class StreamGambleConfig {
    var enabled: Boolean = false

    // Start a round automatically whenever someone pays you at least [minStake].
    var autoStart: Boolean = true

    // Chance that the host (you) wins a round, in percent.
    var hostChancePercent: Int = 60

    // What a winning player gets back for their stake (2.0 = stake returned plus the same again).
    var payoutMultiplier: Float = 2.0f

    var minStake: Long = 1_000L

    // Shown as the host. Empty = your own Minecraft name.
    var hostName: String = ""

    var soundEnabled: Boolean = true

    var scale: Float = 1.0f

    // Vertical position of the overlay as a fraction of the screen height.
    var y: Float = 0.12f
}
