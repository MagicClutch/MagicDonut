package de.magicclutch.magicdonut.client.auction

import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

/** The auction's own sounds (see `assets/magicdonut/sounds.json`). */
object AuctionSounds {
    private fun event(name: String): SoundEvent = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("magicdonut", name))

    val TICK = event("auction_tick")
    val DIM = event("auction_dim")
    val BELL = event("auction_bell")

    private const val SPIN_MS = 1000L

    /**
     * How hard the timer shakes: 0 above 10 seconds, then growing to 1 as the time runs out. The returned pair is a
     * jittery offset (-1..1 on both axes) times that strength; callers scale it to their units.
     */
    fun shake(auction: AuctionManager.Auction?, now: Long = System.currentTimeMillis()): Pair<Float, Float> {
        if (auction == null || AuctionManager.current !== auction) return 0f to 0f
        val left = (auction.endsAtMs - now) / 1000f
        if (left > 10f || left < 0f) return 0f to 0f
        val strength = (1f - left / 10f).let { it * it }   // gentle at first, wild at the end
        val t = now / 1000.0
        val dx = (Math.sin(t * 47.0) * 0.6 + Math.sin(t * 83.0) * 0.4).toFloat()
        val dy = (Math.cos(t * 53.0) * 0.6 + Math.sin(t * 71.0) * 0.4).toFloat()
        return dx * strength to dy * strength
    }

    /** Extra rotation in radians after a new top bid: one full turn, eased, then 0 again. */
    fun spinAngle(lastBidAtMs: Long, now: Long = System.currentTimeMillis()): Float {
        val elapsed = now - lastBidAtMs
        if (lastBidAtMs <= 0L || elapsed !in 0 until SPIN_MS) return 0f
        val p = elapsed / SPIN_MS.toFloat()
        val eased = p * p * (3f - 2f * p)
        return (eased * 2 * Math.PI).toFloat()
    }
}
