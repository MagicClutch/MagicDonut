package de.magicclutch.magicdonut.client.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import de.magicclutch.magicdonut.client.earnings.EarningsTracker
import de.magicclutch.magicdonut.client.util.Money
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyColumn
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.oneconfig.api.config.v1.annotations.Button
import org.polyfrost.oneconfig.api.hud.v1.Hud
import java.time.format.DateTimeFormatter

/**
 * Earnings statistics as a normal OneConfig HUD: a line graph of the last 30 days, the best and worst day and the top
 * payers. Turn it on in the HUD editor; it replaces the old statistics window.
 */
class EarningsStatsHud : Hud("magicdonut_earnings_stats", "Earnings Statistics", Category.INFO) {
    private class Snapshot(
        val today: Double,
        val days: List<Double>,
        val first: String,
        val last: String,
        val best: String?,
        val worst: String?,
        val payers: List<Pair<String, Double>>,
    )

    @Button(title = "Reset earnings", description = "Deletes the recorded earnings shown by this HUD.", text = "Reset")
    fun resetEarnings() = EarningsTracker.reset()

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.")
    private var snapshot: MutableState<Snapshot> = mutableStateOf(buildSnapshot())

    init {
        padLeft = 6f
        padRight = 6f
        padTop = 5f
        padBottom = 5f
    }

    private fun buildSnapshot(): Snapshot {
        val days = EarningsTracker.lastDays(30)
        val withData = days.filter { it.second > 0 }
        val best = withData.maxByOrNull { it.second }?.let { "Best day: ${it.first.format(dateFormat)} ${Money.format(it.second)}" }
        val worst = withData.minByOrNull { it.second }?.let { "Worst day: ${it.first.format(dateFormat)} ${Money.format(it.second)}" }
        return Snapshot(
            EarningsTracker.today(), days.map { it.second },
            days.first().first.format(dateFormat), days.last().first.format(dateFormat),
            best, worst, EarningsTracker.topPayers(5),
        )
    }

    private fun same(a: Snapshot, b: Snapshot) =
        a.today == b.today && a.days == b.days && a.best == b.best && a.worst == b.worst && a.payers == b.payers && a.last == b.last

    override fun update(): Boolean {
        val next = buildSnapshot()
        if (same(next, snapshot.value)) return false
        snapshot.value = next
        return true
    }

    /** Checked every two seconds (in nanoseconds). */
    override fun updateFrequency(): Long = 2_000_000_000L

    @Composable
    override fun Content() {
        val s = snapshot.value
        val scale = textScale
        val pad = PolyInsets(padLeft * scale, padTop * scale, padRight * scale, padBottom * scale)
        val graphW = 140f * scale
        val graphH = 44f * scale

        PolyBox(modifier = hudBackground().padding(pad)) {
            PolyColumn(gap = 3f * scale) {
                PolyMcText(text = "Earnings today: ${Money.format(s.today)}", color = PolyColor(GREEN), scale = scale)

                // The 30-day line graph with a faint area below the line, drawn as a vector shape.
                PolyCanvas(modifier = PolyModifier.size(graphW, graphH)) { x, y, w, h ->
                    rect(x, y, w, h, PolyColor(0x55000000), 3f * scale)
                    val max = (s.days.maxOrNull() ?: 0.0).coerceAtLeast(1.0).toFloat()
                    fun px(i: Int) = x + 3f * scale + (w - 6f * scale) * i / (s.days.size - 1)
                    fun py(v: Double) = y + h - 3f * scale - (h - 6f * scale) * (v.toFloat() / max)
                    val area = PolyPath().apply {
                        moveTo(px(0), y + h - 3f * scale)
                        s.days.forEachIndexed { i, v -> lineTo(px(i), py(v)) }
                        lineTo(px(s.days.size - 1), y + h - 3f * scale)
                        close()
                    }
                    fillPath(area, PolyColor(0x3355FF55))
                    for (i in 0 until s.days.size - 1) {
                        line(px(i), py(s.days[i]), px(i + 1), py(s.days[i + 1]), PolyColor(GREEN), 1.2f * scale)
                    }
                }
                PolyRow(gap = 0f) {
                    PolyMcText(text = s.first, color = PolyColor(GRAY), scale = scale * 0.8f)
                    PolySpacerWidth(graphW - (net.minecraft.client.Minecraft.getInstance().font.width(s.first + s.last) * scale * 0.8f))
                    PolyMcText(text = s.last, color = PolyColor(GRAY), scale = scale * 0.8f)
                }

                if (s.best == null) {
                    PolyMcText(text = "No payments recorded yet", color = PolyColor(GRAY), scale = scale)
                } else {
                    PolyMcText(text = s.best, color = PolyColor(GREEN), scale = scale)
                    PolyMcText(text = s.worst ?: "", color = PolyColor(RED), scale = scale)
                }
                s.payers.forEachIndexed { i, (name, amount) ->
                    PolyMcText(text = "${i + 1}. $name  ${Money.format(amount)}", scale = scale)
                }
            }
        }
    }

    override fun showByDefault(): Boolean = false

    override fun defaultPosition(): Pair<Float, Float> = 10f to 90f

    override fun minimumSize(): Pair<Float, Float> = 100f to 50f

    override fun setup() {
        super.setup()
        update()
    }

    override fun clone(): Hud = (super.clone() as EarningsStatsHud).also { it.snapshot = mutableStateOf(it.buildSnapshot()) }

    companion object {
        private const val GREEN = 0xFF55FF55.toInt()
        private const val RED = 0xFFFF5555.toInt()
        private const val GRAY = 0xFFAAAAAA.toInt()
    }
}

@Composable
private fun PolySpacerWidth(width: Float) {
    org.polyfrost.compose.composables.PolySpacer(width = width.coerceAtLeast(0f))
}
