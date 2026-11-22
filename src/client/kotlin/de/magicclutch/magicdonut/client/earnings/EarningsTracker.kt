package de.magicclutch.magicdonut.client.earnings

import com.google.gson.GsonBuilder
import de.magicclutch.magicdonut.client.payment.Payment
import de.magicclutch.magicdonut.client.payment.PaymentListener
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.time.LocalDate

/** Remembers how much each player paid you, in total and per day. Stored in `magicdonututils-earnings.json`. */
object EarningsTracker {
    private class Store {
        var byPlayer: MutableMap<String, Double>? = null
        var byDay: MutableMap<String, Double>? = null
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = FabricLoader.getInstance().configDir.resolve("magicdonututils-earnings.json")

    private val byPlayer = HashMap<String, Double>()
    private val byDay = HashMap<String, Double>()

    fun init() {
        load()
        PaymentListener.onReceived { onPayment(it) }
    }

    private fun onPayment(payment: Payment) {
        synchronized(this) {
            byPlayer.merge(payment.player, payment.amount, Double::plus)
            byDay.merge(LocalDate.now().toString(), payment.amount, Double::plus)
        }
        save()
    }

    @Synchronized
    fun today(): Double = byDay[LocalDate.now().toString()] ?: 0.0

    @Synchronized
    fun topPayers(n: Int): List<Pair<String, Double>> =
        byPlayer.entries.sortedByDescending { it.value }.take(n).map { it.key to it.value }

    /** The last [days] days, oldest first (days without payments are 0). */
    @Synchronized
    fun lastDays(days: Int): List<Pair<LocalDate, Double>> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()).let { d -> d to (byDay[d.toString()] ?: 0.0) } }
    }

    @Synchronized
    fun reset() {
        byPlayer.clear()
        byDay.clear()
        save()
    }

    private fun load() {
        try {
            if (!Files.exists(file)) return
            val store = Files.newBufferedReader(file).use { gson.fromJson(it, Store::class.java) } ?: return
            store.byPlayer?.let { byPlayer.putAll(it) }
            store.byDay?.let { byDay.putAll(it) }
        } catch (e: Exception) {
            System.err.println("[MagicDonutUtils] Could not read earnings file: $e")
        }
    }

    @Synchronized
    private fun save() {
        try {
            val store = Store().also { it.byPlayer = byPlayer; it.byDay = byDay }
            Files.newBufferedWriter(file).use { gson.toJson(store, it) }
        } catch (e: Exception) {
            System.err.println("[MagicDonutUtils] Could not save earnings file: $e")
        }
    }
}
