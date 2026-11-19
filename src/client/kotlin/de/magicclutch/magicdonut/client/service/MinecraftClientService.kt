package de.magicclutch.magicdonut.client.service

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object MinecraftClientService {
    private const val DONUT_DOMAIN = "donutsmp.net"

    val client: Minecraft
        get() = Minecraft.getInstance()

    val isOnDonut: Boolean
        get() {
            val address = client.currentServer?.ip ?: return false
            // Strip an optional port and a trailing dot (fully qualified form).
            val host = address.trim().lowercase().substringBefore(':').trimEnd('.')
            return host == DONUT_DOMAIN || host.endsWith(".$DONUT_DOMAIN")
        }

    fun sendChatMessage(component: Component) {
        client.gui.chatListener().handleSystemMessage(component, false)
    }
}
