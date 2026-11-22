package de.magicclutch.magicdonut.client.payment

import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import java.util.concurrent.CopyOnWriteArrayList

/** One payment seen in the chat: `You paid Steve $1.5K` ([outgoing]) or `Steve paid you $1.5K`. */
data class Payment(
    val player: String,
    val amount: Double,
    val outgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * The single place that reads payments from the chat. Everything that cares about payments (tracker, earnings,
 * donations, auction) subscribes here instead of parsing messages itself.
 *
 * Only system messages are read (never player chat), and only on DonutSMP. If the server changes the wording,
 * adjust the two patterns below.
 */
object PaymentListener {
    private const val AMOUNT = """\$\s*([\d,]+(?:\.\d+)?)\s*([kmbtKMBT]?)"""
    private val sentRegex = Regex("""^You paid (\S+) $AMOUNT""")
    private val receivedRegex = Regex("""^(\S+) paid you $AMOUNT""")

    private val listeners = CopyOnWriteArrayList<(Payment) -> Unit>()

    /** Calls [listener] for every payment, sent and received. */
    fun subscribe(listener: (Payment) -> Unit) {
        listeners.add(listener)
    }

    /** Like [subscribe], but only for payments you received. */
    fun onReceived(listener: (Payment) -> Unit) = subscribe { if (!it.outgoing) listener(it) }

    fun init() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (overlay || !MinecraftClientService.isOnDonut) return@register
            val payment = parse(message.string) ?: return@register
            for (listener in listeners) {
                // One misbehaving subscriber must not stop the others.
                try {
                    listener(payment)
                } catch (e: Exception) {
                    System.err.println("[MagicDonutUtils] Payment listener failed: $e")
                }
            }
        }
    }

    fun parse(text: String): Payment? {
        // "Sent" first, so a player called "you" can't confuse the received pattern.
        val sent = sentRegex.find(text)
        val match = sent ?: receivedRegex.find(text) ?: return null
        val amount = parseAmount(match.groupValues[2], match.groupValues[3]) ?: return null
        return Payment(match.groupValues[1], amount, outgoing = sent != null)
    }

    private fun parseAmount(number: String, suffix: String): Double? {
        val base = number.replace(",", "").toDoubleOrNull() ?: return null
        val multiplier = when (suffix.lowercase()) {
            "k" -> 1e3
            "m" -> 1e6
            "b" -> 1e9
            "t" -> 1e12
            else -> 1.0
        }
        return base * multiplier
    }
}
