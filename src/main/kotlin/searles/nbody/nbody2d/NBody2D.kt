package searles.nbody.nbody2d

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import searles.nbody.Commons
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

    private val universe = Universe(G = 0.0006, dt = 0.1).apply {
        addAll(
            createRotatingDisc(20000, 1.0, 0.001, 100.0, true).withMotion(0.0, 0.06).moveBy(-2.0, 0.0)
        )
        addAll(
            createRotatingDisc(20000, 1.0, 0.001, 100.0, true).withMotion(0.0, -0.06).moveBy(2.0, 0.0)
        )
    }

    private var viewMatrix = Commons.translate(width / 2.0, height / 2.0)
        .multiply(Commons.scale2D(max(width / 2.0, height / 2.0)))
        .multiply(Commons.translate(universe.bodyStats.cx, universe.bodyStats.cy))
        .multiply(Commons.scale2D(0.5 / sqrt(max(universe.bodyStats.s2x, universe.bodyStats.s2y))))

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
            viewMatrix = Commons.translate(it.x, it.y)
                .multiply(Commons.scale2D(1 + 0.01 * it.deltaY))
                .multiply(Commons.translate(-it.x, -it.y))
                .multiply(viewMatrix)
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

                viewMatrix = Commons.translate(dx, dy).multiply(viewMatrix)
            }
        }
    }

    private fun drawUniverse(pixelBuffer: PixelBuffer) {
        pixelBuffer.fill(0xff000000.toInt())
        val pt = doubleArrayOf(0.0, 0.0, 1.0)

        universe.forEachBody {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = getSizeForMass(it.mass, universe.bodyStats.minMass, universe.bodyStats.maxMass)
            val color = getColorForStats(ln(it.totalForce), universe.bodyStats.meanLogForce, sqrt(universe.bodyStats.varianceLogForce))

            pt[0] = it.x
            pt[1] = it.y

            val resultVector = viewMatrix.operate(pt)

            //val vx = (it.x - cx + lenX / 2.0) / lenX * pixelBuffer.width
            //val vy = (it.y - cy + lenY / 2.0) / lenY * pixelBuffer.height
            pixelBuffer.drawCircle(resultVector[0], resultVector[1], size, color)
        }
    }
}