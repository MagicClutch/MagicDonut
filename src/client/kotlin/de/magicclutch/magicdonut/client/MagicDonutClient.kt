package de.magicclutch.magicdonut.client

import de.magicclutch.magicdonut.client.auction.AuctionCommand
import de.magicclutch.magicdonut.client.auction.AuctionHologram
import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.auction.AuctionRenderer
import de.magicclutch.magicdonut.client.config.ConfigCommand
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.DonationCommand
import de.magicclutch.magicdonut.client.donation.DonationManager
import de.magicclutch.magicdonut.client.hud.BalanceHud
import de.magicclutch.magicdonut.client.hud.DonationHud
import de.magicclutch.magicdonut.client.hud.ProfitHud
import de.magicclutch.magicdonut.client.overheadstats.StatsRenderer
import de.magicclutch.magicdonut.client.payconfirm.PayConfirmClient
import de.magicclutch.magicdonut.client.profit.ProfitCommand
import de.magicclutch.magicdonut.client.profit.ProfitTracker
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.hud.v1.HudManager

class MagicDonutClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> ConfigCommand.registerCommands(dispatcher) }

        ClientReceiveMessageEvents.GAME.register { component, _ ->
            val content = component.string

            if(MagicDonutConfig.instance.developmentConfig.apiKey.isNotBlank() || !MinecraftClientService.isOnDonut) {
                return@register
            }

            val matcher = APIService.apiTokenPattern.matcher(content)
            if(matcher.find()) {
                val apiKey = matcher.group(2)
                MagicDonutConfig.instance.developmentConfig.apiKey = apiKey
                MagicDonutConfig.save()
                MinecraftClientService.sendChatMessage(
                    Component.literal("[MagicDonutUtils] API-Key saved ✓").withStyle(ChatFormatting.GREEN)
                )
            }
        }

        ClientReceiveMessageEvents.GAME.register { message, _ ->
            DonationManager.handleChatMessage(message)
        }

        StatsRenderer.init()
        PayConfirmClient.init()
        DonationCommand.register()
        HudManager.register(DonationHud(), BalanceHud(), ProfitHud())
        AuctionManager.init()
        AuctionRenderer.register()
        AuctionHologram.register()
        AuctionCommand.register()
        ProfitTracker.init()
        ProfitCommand.register()
    }

}
