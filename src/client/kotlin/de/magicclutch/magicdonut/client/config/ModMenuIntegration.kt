package de.magicclutch.magicdonut.client.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import de.magicclutch.magicdonut.client.gui.MagicScreen

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent -> MagicScreen(parent) }
}
