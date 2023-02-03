package searles.nbody.nbody3d

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import searles.nbody.Commons
import searles.nbody.DirectImageWriter
import searles.nbody.PixelBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody3D: DirectImageWriter() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)
    private val universe = Universe(G = 0.006, dt = 0.01).apply {
        this.addAll(Universe.createCloud(10000, 1.0, 0.1, 0.1))
    }

    private var cx: Double = universe.bodyStats.cx
    private var cy: Double = universe.bodyStats.cy
    private var cz: Double = universe.bodyStats.cz
    private var len: Double = sqrt(max(universe.bodyStats.s2x, universe.bodyStats.s2y))

    private val lenX get() = if(width < height) len else len * width / height
    private val lenY get() = if(width > height) len else len * height / width

    override fun animationStep(now: Long, pixelBuffer: PixelBuffer) {
        @Suppress("DeferredResultUnused")
        GlobalScope.async {
            // code to run in the background
            if (isCalculationRunning.compareAndSet(false, true)) {
                universe.step()
                Platform.runLater {
                    drawUniverse(pixelBuffer)
                    updateView()
                    isCalculationRunning.set(false)
                }
            }
        }
    }

    private fun drawUniverse(pixelBuffer: PixelBuffer) {
        pixelBuffer.fill(0xff000000.toInt())

        universe.forEachBody {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = Commons.getSizeForMass(it.mass, universe.bodyStats.minMass, universe.bodyStats.maxMass)
            val vx = (it.x - cx + lenX / 2.0) / lenX * pixelBuffer.width
            val vy = (it.y - cy + lenY / 2.0) / lenY * pixelBuffer.height

            val color = Commons.getColorForStats(
                ln(it.totalForce),
                universe.bodyStats.meanLogForce,
                sqrt(universe.bodyStats.varianceLogForce)
            )

            pixelBuffer.drawCircle(vx, vy, size, color)
        }
    }

    override fun connectControls(scene: Scene) {
        scene.onScroll = EventHandler {
            len -= len * 0.01 * it.deltaY
        }

        var isDragged = false
        var x0 = 0.0
        var y0 = 0.0

        scene.onMousePressed = EventHandler { event ->
            isDragged = true
            x0 = event.x
            y0 = event.y
        }

        scene.onMouseReleased = EventHandler {
            isDragged = false
        }

        scene.onMouseDragged = EventHandler {
            if(isDragged) {
                val dx = it.x - x0
                val dy = it.y - y0

                x0 = it.x
                y0 = it.y

                cx -= (dx / width) * lenX
                cy -= (dy / height) * lenY
            }
        }
    }
}