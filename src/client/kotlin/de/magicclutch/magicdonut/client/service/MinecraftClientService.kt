package de.magicclutch.magicdonut.client.service

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object MinecraftClientService {
    val domainRegex = Regex("(\\w*\\.)?donutsmp\\.net")

    val client: Minecraft = Minecraft.getInstance()

    val isOnDonut: Boolean
        get() {
            return client.currentServer?.ip?.matches(domainRegex) ?: false
        }

    fun sendChatMessage(component: Component) {
        client.chatListener.handleSystemMessage(
            component,
            false
        )
    }
}