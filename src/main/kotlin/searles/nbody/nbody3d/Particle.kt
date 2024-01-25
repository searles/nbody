package searles.nbody.nbody3d

import kotlin.math.min
import kotlin.math.pow

data class Particle(
    val pt: MassPoint,
    var vel: Vec = Vec(),
    var acc: Vec = Vec(),
    var totalForce: Double = 0.0
) {
    fun resetTotalForce() {
        acc.setToZero()
        totalForce = 0.0
    }

    fun move(dt: Double) {
        vel.add(acc, dt)
        pt.pos.moveBy(vel, dt)
    }

    fun addForce(other: MassPoint, G: Double) {
        if(other == pt) return

        val distance = pt.pos.distance(other.pos) + 1e-9

        val force = (pt.mass * other.mass * G) / distance.pow(2)
        val a = force / pt.mass

        // the change of velocity should be smaller than d because it is into the direction of x2/y2.
        acc.add(other.pos, pt.pos, min(a / distance, distance))
        totalForce += force // XXX or 'a'?
    }

    override fun toString(): String {
        return "{$pt, vel=$vel, acc=$acc, totalForce=$totalForce}"
    }
}