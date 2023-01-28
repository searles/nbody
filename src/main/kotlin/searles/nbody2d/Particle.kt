package searles.nbody2d

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow

class Particle(
    var x: Double,
    var y: Double,
    val m: Double,
    var vx: Double,
    var vy: Double
) {
    var totalForce: Double = 0.0 // special tweak, total force applied to this fellow.
        private set

    fun resetTotalForce() {
        totalForce = 0.0
    }

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

        // the change of velocity should be smaller than d because it is into the direction of x2/y2.
        val ft = min(a * dt, 2 * distance)

        vx += d0x * ft
        vy += d0y * ft
        totalForce += force
    }
}

