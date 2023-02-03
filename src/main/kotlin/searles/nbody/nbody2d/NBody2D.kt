package searles.nbody.nbody2d

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import searles.nbody.Commons.getColorForStats
import searles.nbody.Commons.getSizeForMass
import searles.nbody.DirectImageWriter
import searles.nbody.PixelBuffer
import searles.nbody.nbody2d.Universe.Companion.createRotatingDisc
import searles.nbody.nbody2d.Universe.Companion.moveBy
import searles.nbody.nbody2d.Universe.Companion.withMotion
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody2D : DirectImageWriter() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)

    val universe = Universe(G = 0.0006, dt = 0.01).apply {
        addAll(
            createRotatingDisc(20000, 1.0, 0.01, 100.0, true).withMotion(0.0, 0.01).moveBy(-2.0, 0.0)
        )
        addAll(
            createRotatingDisc(20000, 1.0, 0.01, 100.0, true).withMotion(0.0, -0.01).moveBy(2.0, 0.0)
        )
    }

    private var cx: Double = universe.bodyStats.cx
    private var cy: Double = universe.bodyStats.cy
    private var len: Double = sqrt(max(universe.bodyStats.s2x, universe.bodyStats.s2y))

    private val lenX get() = if(width < height) len else len * width / height
    private val lenY get() = if(width > height) len else len * height / width

    override fun animationStep(now: Long, pixelBuffer: PixelBuffer) {
        @Suppress("DeferredResultUnused")
        GlobalScope.async {
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

    private fun drawUniverse(pixelBuffer: PixelBuffer) {
        pixelBuffer.fill(0xff000000.toInt())

        universe.forEachBody {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = getSizeForMass(it.mass, universe.bodyStats.minMass, universe.bodyStats.maxMass)
            val vx = (it.x - cx + lenX / 2.0) / lenX * pixelBuffer.width
            val vy = (it.y - cy + lenY / 2.0) / lenY * pixelBuffer.height
            val color = getColorForStats(ln(it.totalForce), universe.bodyStats.meanLogForce, sqrt(universe.bodyStats.varianceLogForce))
            pixelBuffer.drawCircle(vx, vy, size, color)
        }
    }
}