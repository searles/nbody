package searles.nbody.nbody3d

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class Vec(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    fun manhattanDistance(other: Vec): Double {
        return max(max(abs(x - other.x), abs(y - other.y)), abs(z - other.z))
    }

    fun distance(other: Vec): Double {
        return sqrt((x - other.x).pow(2) + (y - other.y).pow(2) + (z - other.z).pow(2))
    }

    fun setToZero() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    fun moveBy(vel: Vec, dt: Double) {
        x += vel.x * dt
        y += vel.y * dt
        z += vel.z * dt
    }

    fun add(dst: Vec, src: Vec, factor: Double): Vec {
        x += (dst.x - src.x) * factor
        y += (dst.y - src.y) * factor
        z += (dst.z - src.z) * factor

        return this
    }

    fun add(dir: Vec, factor: Double): Vec {
        x += dir.x * factor
        y += dir.y * factor
        z += dir.z * factor

        return this
    }

    fun setToMiddle(v1: Vec, v2: Vec) {
        x = (v1.x + v2.x) / 2.0
        y = (v1.y + v2.y) / 2.0
        z = (v1.z + v2.z) / 2.0
    }

    fun setTo(other: Vec) {
        x = other.x
        y = other.y
        z = other.z
    }

    fun divideBy(factor: Double) {
        x /= factor
        y /= factor
        z /= factor
    }

    override fun toString(): String {
        return "($x, $y, $z)"
    }

    fun length(): Double {
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    fun applyToComponents(action: (Double) -> Unit) {
        action(x)
        action(y)
        action(z)
    }

    companion object {

    }
}