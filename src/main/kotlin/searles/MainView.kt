package searles

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import tornadofx.*
import java.lang.Math.random
import kotlin.math.*

class MainView : View() {
    private val width = 800.0
    private val height = 800.0
    var cx: Double = 0.0
    var cy: Double = 0.0
    var len: Double = 1.0

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

    val universe = createSolarSystem().also {
        it.updateCenter()
        it.updateVariance()
        cx = it.centerX
        cy = it.centerY
        len = it.variance
    }

    private fun color(d: Double): Color {
        val r = sin(d * 2 * Math.PI) / 2.0 + 0.5
        val g = sin(d * 2 * Math.PI + 2) / 2.0 + 0.5
        val b = sin(d * 2 * Math.PI + 4) / 2.0 + 0.5
        val a = 1.0

        return c(r, g, b, a)
    }

    override val root = vbox {
        children.addAll(listOf(canvas))

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                val gc = canvas.graphicsContext2D

                gc.fill = c("black")
                gc.fillRect(0.0, 0.0, width, height)

                universe.forEachParticle {
                    gc.fill = color(it.color)
                    val vx = (it.x - cx + len / 2.0) / len * width
                    val vy = (it.y - cy + len / 2.0) / len * height
                    gc.fillRect(vx, vy, 1.0, 1.0)
                }

                gc.fill = c("black")

                universe.step()
            }
        }
        timer.start()
    }

    private fun createSolarSystem(): Universe {
        return Universe(G = 6.674e-11, dt = 36000.0).apply {
            addParticle(Particle(0.0, 0.0, 0.0, 0.0, 1.989e30)) // sun
            addParticle(Particle(149.6e9, 0.0, 0.0, 29780.0, 5.972e24)) // earth
            addParticle(Particle(149.6e9 + 384400000, 0.0, 0.0, 29780.0 + 1022, 7.348e22)) // moon
        }
    }

    private fun createSpiral(): Universe {
        // initial velocity = sqrt(M / r)
        val centerMass = 1e7

        return Universe(G = 1.0, dt = 1.0).apply {
            addParticle(Particle(0.0, 0.0, 0.0, 0.0, centerMass)) // sun

            for(i in 2 .. 2000) {
                val r = i.toDouble() * 10
                val v = sqrt(centerMass / r)
                addParticle(Particle(r, 0.0, 0.0, v, 0.001))
            }
        }
    }

    private fun createBlackHole(): Universe {
        return Universe(G = 1.0, dt = 100.0).apply {
            val massBlackHole = 1e20

            repeat(3000) {
                val r = random() * 1e10
                val arc = random() * 2 * PI
                val mass = 1e9
                val x = cos(arc) * r
                val y = sin(arc) * r
                val v = 0.0//sqrt(massBlackHole / r) * random()
                val vx = sin(arc) * v
                val vy = -cos(arc) * v

                addParticle(Particle(x, y, vx, vy, mass))
            }

            addParticle(Particle(1e10, 0.0, 0.0, 0.0, massBlackHole))
        }
    }

    private fun createBlackHoleWithMovingParticles(): Universe {
        return Universe(G = 1.0, dt = 100.0).apply {
            val massBlackHole = 2e20
            val massParticle = 1e12
            val radCloud = 1e10
            val cloudSpeed = 2e5
            val yDelta = 0.0

            repeat(1000) {
                val color = random()
                val r = random() * radCloud
                val arc = color * PI * if (random() > 0.5) 1 else -1
                val mass = massParticle
                val x = cos(arc) * r - radCloud
                val y = sin(arc) * r - yDelta
                val vx = cloudSpeed
                val vy = 0.0

                addParticle(Particle(x, y, vx, vy, mass, color))
            }

            addParticle(Particle(0.0, 0.0, 0.0, 0.0, massBlackHole))
        }
    }

    private fun createTwoRotatingBlackStars(): Universe {
        return Universe(G = 1.0, dt = 0.01).apply {
            val massBlackHole = 1e30
            val massSmallerBlackHole = 1e20
            val distanceBlackHoles = 1e8

            val v = sqrt(massBlackHole / distanceBlackHoles)

            addParticle(Particle(-distanceBlackHoles, 0.0, 0.0, -v, massSmallerBlackHole))
            addParticle(Particle(distanceBlackHoles, 0.0, 0.0, v, massSmallerBlackHole))
            addParticle(Particle(0.0, 0.0, 0.0, 0.0, massBlackHole))

            /*repeat(3000) {
                val r = random() * 1e10
                val arc = random() * 2 * PI
                val mass = 1e9
                val x = cos(arc) * r - 2e10
                val y = sin(arc) * r
                val vx = 2e5
                val vy = 0.0

                addParticle(Particle(x, y, vx, vy, mass))
            }*/
        }
    }
}