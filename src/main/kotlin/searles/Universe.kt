package searles

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class Universe(val G: Double, val dt: Double = 1.0) {
    var centerX: Double = 0.0
        private set
    var centerY: Double = 0.0
        private set
    var variance: Double = 1.0
        private set

    private val particles = mutableListOf<Particle>()

    fun forEachParticle(action: (Particle) -> Unit) {
        particles.forEach(action)
    }

    fun addParticle(p: Particle) {
        particles.add(p)
    }

    fun step() {
        for(i in particles.indices) {
            for(j in i + 1 until particles.size) {
                adjustVelocity(particles[i], particles[j])
            }
        }

        particles.forEach {
            moveParticle(it)
        }

        updateCenter()
        updateVariance()
    }

    fun updateCenter() {
        if (particles.isEmpty()) return

        var x = 0.0
        var y = 0.0
        var totalMass = 0.0

        particles.forEach {
            x += it.x * it.m
            y += it.y * it.m

            totalMass += it.m
        }

        x /= (particles.size * totalMass)
        y /= (particles.size * totalMass)

        centerX = x
        centerY = y
    }

    fun updateVariance() {
        if(particles.size <= 1) return

        var s2x = 0.0
        var s2y = 0.0

        particles.forEach {
            s2x += (it.x - centerX).pow(2)
            s2y += (it.y - centerY).pow(2)
        }

        s2x /= particles.size - 1
        s2y /= particles.size - 1

        variance = sqrt(max(s2x, s2y))
    }

    private fun adjustVelocity(p1: Particle, p2: Particle) {
        val distance = hypot(p1.x - p2.x, p1.y - p2.y)
        val d0x = (p2.x - p1.x) / distance
        val d0y = (p2.y - p1.y) / distance

        val force = (p1.m * p2.m * G) / distance.pow(2)

        val a1 = force / p1.m
        val a2 = force / p2.m

        p1.vx += a1 * d0x * dt
        p1.vy += a1 * d0y * dt
        p2.vx -= a2 * d0x * dt
        p2.vy -= a2 * d0y * dt
    }

    private fun moveParticle(p: Particle) {
        p.x += p.vx * dt
        p.y += p.vy * dt
    }
}