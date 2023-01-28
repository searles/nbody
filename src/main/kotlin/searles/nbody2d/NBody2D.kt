package searles.nbody2d

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody2D : Application() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)

    private val width = 800.0
    private val height = 800.0

    val universe = Universe.createParticleSuckingCloud()

    private var cx: Double = universe.centerX
    private var cy: Double = universe.centerY
    private var len: Double = universe.getStandardDeviation() * 4

    override fun start(primaryStage: Stage) {
        val canvas = Canvas(width, height)
        connectCanvas(canvas)

        val root = Group()
        root.children.add(canvas)
        val scene = Scene(root, width, height)
        primaryStage.scene = scene
        primaryStage.show()

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                @Suppress("DeferredResultUnused")
                GlobalScope.async {
                    // code to run in the background
                    if (isCalculationRunning.compareAndSet(false, true)) {
                        universe.step()
                        Platform.runLater {
                            drawUniverse(canvas)
                            isCalculationRunning.set(false)
                        }
                    }
                }
            }
        }

        timer.start()
    }

    private fun connectCanvas(canvas: Canvas) {
        canvas.onScroll = EventHandler {
            len -= len * 0.01 * it.deltaY
        }

        var isDragged = false
        var x0 = 0.0
        var y0 = 0.0

        canvas.onMousePressed = EventHandler { event ->
            isDragged = true
            x0 = event.x
            y0 = event.y
        }

        canvas.onMouseReleased = EventHandler {
            isDragged = false
        }

        canvas.onMouseDragged = EventHandler {
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

    private fun drawUniverse(canvas: Canvas) {
        val graphicsContext = canvas.graphicsContext2D

        graphicsContext.fill = Color.BLACK
        graphicsContext.fillRect(0.0, 0.0, width, height)

        var minMass = universe.bodies.first().mass
        var maxMass = universe.bodies.first().mass
        var meanLogForce = 0.0
        var varianceLogForce = 0.0
        var n = 0

        universe.forEachBody {
            n++

            minMass = min(minMass, it.mass)
            maxMass = max(maxMass, it.mass)

            val logForce = ln(it.totalForce)

            varianceLogForce = (n - 1.0) / n * (varianceLogForce + meanLogForce.pow(2)) + logForce.pow(2) / n
            meanLogForce = (n - 1.0) / n * meanLogForce + logForce / n
            varianceLogForce -= meanLogForce.pow(2)
        }

        universe.forEachBody {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = getSizeForMass(it.mass, minMass, maxMass)
            graphicsContext.fill = getColorForStats(ln(it.totalForce), meanLogForce, sqrt(varianceLogForce))
            val vx = (it.x - cx + len / 2.0) / len * width
            val vy = (it.y - cy + len / 2.0) / len * height
            graphicsContext.fillOval(vx, vy, size, size)
        }

        graphicsContext.fill = Color.WHITE
        graphicsContext.fillText(universe.time.toString(), 0.0, 10.0)

        graphicsContext.fill = Color.BLACK
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
        val maxSize = 2.0

        val d = maxMass - minMass
        if(d == 0.0) return 1.0

        val min = Math.cbrt(minMass)
        val max = Math.cbrt(maxMass)
        val len = Math.cbrt(mass)

        return (maxSize - minSize) * (len - min) / (max - min) + minSize
    }
}