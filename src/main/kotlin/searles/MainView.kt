package searles

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class MainView : View() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)

    private val width = 800.0
    private val height = 800.0

    val universe = Universe.createCollidingDiscs()

    var cx: Double = universe.centerX
    var cy: Double = universe.centerY
    var len: Double = universe.getStandardDeviation() * 4

    private val canvas = Canvas(width, height).apply {
        onScroll = EventHandler {
            len -= len * 0.01 * it.deltaY
        }

        var isDragged = false
        var x0 = 0.0
        var y0 = 0.0

        onMousePressed = EventHandler { event ->
            isDragged = true
            x0 = event.x
            y0 = event.y
        }

        onMouseReleased = EventHandler {
            isDragged = false
        }

        onMouseDragged = EventHandler {
            if(isDragged) {
                val dx = it.x - x0
                val dy = it.y - y0

                x0 = it.x
                y0 = it.y

                cx -= (dx / width) * len
                cy -= (dy / height) * len
            }
        }
    }

    override val root = vbox {
        children.addAll(
            listOf(canvas)
        )

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                @Suppress("DeferredResultUnused")
                GlobalScope.async {
                    // code to run in the background
                    if(isCalculationRunning.compareAndSet(false, true)) {
                        universe.step()
                        runLater {
                            drawUniverse()
                            isCalculationRunning.set(false)
                        }
                    }
                }
            }
        }

        timer.start()
    }

    private fun drawUniverse() {
        val gc = canvas.graphicsContext2D

        gc.fill = c("black")
        gc.fillRect(0.0, 0.0, width, height)

        var minMass = universe.particles.first().m
        var maxMass = universe.particles.first().m
        var meanLogForce = 0.0
        var varianceLogForce = 0.0
        var n = 0

        universe.forEachParticle {
            n++

            minMass = min(minMass, it.m)
            maxMass = max(maxMass, it.m)

            val logForce = ln(it.totalForce)

            varianceLogForce = (n - 1.0) / n * (varianceLogForce + meanLogForce.pow(2)) + logForce.pow(2) / n
            meanLogForce = (n - 1.0) / n * meanLogForce + logForce / n
            varianceLogForce -= meanLogForce.pow(2)
        }

        universe.forEachParticle {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = getSizeForMass(it.m, minMass, maxMass)
            gc.fill = getColorForStats(ln(it.totalForce), meanLogForce, sqrt(varianceLogForce))
            val vx = (it.x - cx + len / 2.0) / len * width
            val vy = (it.y - cy + len / 2.0) / len * height
            gc.fillOval(vx, vy, size, size)
        }

        gc.fill = Color.WHITE
        gc.fillText(universe.time.toString(), 0.0, 10.0)

        gc.fill = c("black")
    }

    private fun getColorForStats(x: Double, mean: Double, sigma: Double): Color {
        val zValue = 2.33 // 99%
        val min = mean - sigma * zValue
        var v = (x - min) / (2 * zValue * sigma)

        if(v < 0.0) v = 0.0 else if(v > 1.0) v = 1.0

        return getColor(v)
    }

    private fun getColor(v: Double): Color {
        // 0 = (0.5, 0.25, 0.0), 0.33 = (1.0, 0.75, ?), 0.66 = (0.5, 0.75, 1.0), 1 = (1.0, 1.0, 1.0)
        val r = 0.5 - 0.5 * cos(v * Math.PI * 3.0)
        val g = 0.3 + 0.7 * v
        val b = if(v < 2.0 / 3.0)
            0.5 - 0.5 * cos(v * Math.PI * 3.0 / 2.0)
        else
            1.0

        return Color.color(r, g, b)
    }

    private fun getSizeForMass(mass: Double, minMass: Double, maxMass: Double): Double {
        val minSize = 0.5
        val maxSize = 4.0

        val d = maxMass - minMass
        if(d == 0.0) return 1.0

        val min = Math.cbrt(minMass)
        val max = Math.cbrt(maxMass)
        val len = Math.cbrt(mass)

        return (maxSize - minSize) * (len - min) / (max - min) + minSize
    }
}