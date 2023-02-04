package searles.nbody.nbody3d

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import searles.nbody.Commons
import searles.nbody.DirectImageWriter
import searles.nbody.PixelBuffer
import searles.nbody.nbody3d.Universe.Companion.createGalaxy
import searles.nbody.nbody3d.Universe.Companion.createRotatingCloud
import searles.nbody.nbody3d.Universe.Companion.moveBy
import searles.nbody.nbody3d.Universe.Companion.withMotion
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody3D: DirectImageWriter() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)
    private val universe = Universe(G = 0.006, dt = 0.001).apply {
//        this.addAll(createRotatingCloud(20000, 4.0, 0.000001, 50.0).withMotion(0.1, 0.4, -0.1).moveBy(-6.0, 0.0, 0.0))
//        this.addAll(createRotatingCloud(20000, 4.0, 0.000001, 50.0).withMotion(0.0, -0.4, 0.1).moveBy(6.0, 0.0, 0.0))
        addAll(listOf(Body(-2.0, -1.0, 0.0, 1000.0, 0.0, 0.2, 0.0)))
        addAll(createGalaxy(20000, 1.0, 0.01, 0.5).moveBy(-2.0, -1.0, 0.0).withMotion(0.0, 0.2, 0.0))
        addAll(listOf(Body(2.0, -1.0, 0.0, 1000.0, 0.0, -0.2, 0.0)))
        addAll(createGalaxy(20000, 1.0, 0.01, 0.5).moveBy(2.0, -1.0, 0.0).withMotion(0.0, -0.2, 0.0))
        addAll(listOf(Body(0.0, 1.0, 0.0, 1000.0, 0.2, 0.0, 0.0)))
        addAll(createGalaxy(20000, 1.0, 0.01, 0.5).moveBy(0.0, 1.0, 0.0).withMotion(0.2, 0.0, 0.0))
    }

    private var viewMatrix = Commons.translate(width / 2.0, height / 2.0, 0.0)
        .multiply(Commons.scale3D(max(width / 2.0, height / 2.0)))

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

        val pt = doubleArrayOf(0.0, 0.0, 0.0, 1.0)

        universe.forEachBody {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = Commons.getSizeForMass(it.mass, universe.bodyStats.minMass, universe.bodyStats.maxMass)
            val color = Commons.getColorForStats(
                ln(it.totalForce),
                universe.bodyStats.meanLogForce,
                sqrt(universe.bodyStats.varianceLogForce)
            )

            pt[0] = it.x
            pt[1] = it.y
            pt[2] = it.z

            val resultVector = viewMatrix.operate(pt)


            // TODO Perspective?

            pixelBuffer.drawCircle(resultVector[0], resultVector[1], size, color)
        }
    }

    override fun connectControls(scene: Scene) {
        scene.onScroll = EventHandler {
            viewMatrix = Commons.translate(it.x, it.y, 0.0)
                .multiply(Commons.scale3D(1 + 0.01 * it.deltaY))
                .multiply(Commons.translate(-it.x, -it.y, 0.0))
                .multiply(viewMatrix)
        }

        var isDragged = false
        var startX = 0.0
        var startY = 0.0

        var x0 = 0.0
        var y0 = 0.0

        scene.onMousePressed = EventHandler { event ->
            isDragged = true
            startX = event.x
            startY = event.y
            x0 = event.x
            y0 = event.y
        }

        scene.onMouseReleased = EventHandler {
            isDragged = false
        }

        scene.onMouseDragged = EventHandler {
            if(!isDragged) return@EventHandler

            val dx = it.x - x0
            val dy = it.y - y0

            x0 = it.x
            y0 = it.y

            if(it.isShiftDown) {
                viewMatrix = Commons.translate(dx, dy, 0.0).multiply(viewMatrix)
            } else {
                viewMatrix = Commons.translate(startX, startY, 0.0)
                    .multiply(Commons.rotate(dx, dy, 0.0))
                    .multiply(Commons.translate(-startX, -startY, 0.0))
                    .multiply(viewMatrix)
            }
        }
    }
}