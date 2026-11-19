package de.magicclutch.magicdonut.client.config.oneconfig

import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint

/**
 * Registered under the `oneconfig:config` entrypoint in fabric.mod.json. OneConfig calls it only when it is
 * installed, which keeps OneConfig a purely optional dependency.
 */
class OneConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        // Creating the config queues it for initialization; it belongs to this mod because its class lives here.
        MagicDonutOneConfig()
    }
}
