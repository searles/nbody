package searles

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean

class MainView : View() {
    private var isCalculationRunning: AtomicBoolean = AtomicBoolean(false)

    private val width = 800.0
    private val height = 800.0

    val universe = Universe.createParticleSuckingCloud()

    var cx: Double = universe.centerX
    var cy: Double = universe.centerY
    var len: Double = universe.getStandardDeviation() * 3

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
                val gc = canvas.graphicsContext2D

                gc.fill = c("black")
                gc.fillRect(0.0, 0.0, width, height)

                universe.forEachParticle {
                    gc.fill = it.color
                    val vx = (it.x - cx + len / 2.0) / len * width
                    val vy = (it.y - cy + len / 2.0) / len * height
                    gc.fillOval(vx, vy, 2.0, 2.0)
                }

                gc.fill = Color.WHITE
                gc.fillText(universe.time.toString(), 0.0, 10.0)

                gc.fill = c("black")

                @Suppress("DeferredResultUnused")
                GlobalScope.async {
                    // code to run in the background
                    if(isCalculationRunning.compareAndSet(false, true)) {
                        universe.step()
                        isCalculationRunning.set(false)
                    }
                }
            }
        }
        timer.start()
    }
}