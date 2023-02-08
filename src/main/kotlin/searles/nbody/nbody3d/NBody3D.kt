package searles.nbody.nbody3d

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import searles.nbody.Commons
import searles.nbody.DirectImageWriter
import searles.nbody.PixelBuffer
import searles.nbody.nbody3d.Universe.Companion.createBall
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class NBody3D: DirectImageWriter() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)
    private val universe = Universe(G = 1.0, dt = 0.001, theta = 2.0).apply {
        val particleCount = 50000
        val massPerParticle = 1e-3
        val massBlackHole = 100.0
//        this.addAll(createRotatingBall(particleCount, 2.0, massPerParticle, massBlackHole).moveBy(-4.0, 0.0, 0.0).withMotion(0.0, -4.0, 0.0))
//        this.addAll(createRotatingBall(particleCount, 2.0, massPerParticle, massBlackHole).moveBy(0.0, -4.0, 0.0).withMotion(4.0, 0.0, 0.0))
//        this.addAll(createRotatingBall(particleCount, 2.0, massPerParticle, massBlackHole).moveBy(4.0, 0.0, 0.0).withMotion(0.0, 4.0, 0.0))
//        this.addAll(createRotatingBall(particleCount, 2.0, massPerParticle, massBlackHole).moveBy(0.0, 4.0, 0.0).withMotion(-4.0, 0.0, 0.0))
        this.addAll(createBall(particleCount, 1.0, massPerParticle))
    }

    private var viewMatrix = Commons.translate(width / 2.0, height / 2.0, 0.0)
        .multiply(Commons.scale3D(max(width / 2.0, height / 2.0)))

    override fun animationStep(now: Long, pixelBuffer: PixelBuffer) {
        @Suppress("DeferredResultUnused")
        GlobalScope.async {
            // code to run in the background
            if (isCalculationRunning.compareAndSet(false, true)) {
                try {
                    universe.step()
                    Platform.runLater {
                        drawUniverse(pixelBuffer)
                        updateView()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isCalculationRunning.set(false)
                }
            }
        }
    }

    private fun drawUniverse(pixelBuffer: PixelBuffer) {
        pixelBuffer.fill(0xff000000.toInt())

        val pt = doubleArrayOf(0.0, 0.0, 0.0, 1.0)

        universe.forEachParticle {
            // size varies from 0.2 to 4 using 3rd root of mass.
            val size = Commons.getSizeForMass(it.pt.mass, universe.stats.minMass, universe.stats.maxMass)
            val color = Commons.getColorForStats(
                ln(it.totalForce),
                universe.stats.meanLogForce,
                sqrt(universe.stats.varianceLogForce)
            )

            pt[0] = it.pt.pos.x
            pt[1] = it.pt.pos.y
            pt[2] = it.pt.pos.z

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