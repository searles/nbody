package searles

import kotlin.math.hypot
import kotlin.math.pow

class Particle(
    var x: Double,
    var y: Double,
    val m: Double,
    var vx: Double,
    var vy: Double,
    val color: Int = 0xffffffff.toInt()
) {
    fun step(dt: Double) {
        x += vx * dt
        y += vy * dt
    }

    fun addForce(x2: Double, y2: Double, m2: Double, G: Double, dt: Double) {
        val distance = hypot(x - x2, y - y2) + 1e-9
        val d0x = (x2 - x) / distance
        val d0y = (y2 - y) / distance

        val force = (m * m2 * G) / distance.pow(2)

        val a = force / m

        vx += a * d0x * dt
        vy += a * d0y * dt
    }
}

