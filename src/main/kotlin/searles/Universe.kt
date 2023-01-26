package searles

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

class Universe(val G: Double, val dt: Double = 1.0, val theta: Double = 0.7) {
    val centerX: Double get() = tree.gx
    val centerY: Double get() = tree.gy

    val bodies = mutableListOf<Body>()

    var time: Double = 0.0
        private set

    private val tree = BalancedBarnesHutTree()

    fun add(body: Body) {
        bodies.add(body)
        tree.add(body)
    }

    suspend fun step() {
        val chunkSize = 500
        val chunks = bodies.chunked(chunkSize)

        coroutineScope {
            val jobs = chunks.map { chunk ->
                async {
                    chunk.forEach { tree.updateForce(it, G, dt, theta) }
                }
            }

            jobs.awaitAll()
        }

        for(body in bodies) {
            tree.step(body, dt)
        }

        time += dt
    }

    fun getStandardDeviation(): Double {
        var s2x = 0.0
        var s2y = 0.0

        bodies.forEach {
            s2x += (it.gx - centerX).pow(2)
            s2y += (it.gy - centerY).pow(2)
        }

        s2x /= bodies.size - 1
        s2y /= bodies.size - 1

        return sqrt(max(s2x, s2y))
    }

    fun forEachBody(action: (Body) -> Unit) {
        for(p in bodies) {
            action(p)
        }
    }

    companion object {
        fun createSolarSystem(): Universe {
            return Universe(G = 6.674e-11, dt = 36000.0).apply {
                add(Body(0.0, 0.0, 1.989e30, 0.0, 0.0)) // sun
                add(Body(149.6e9, 0.0, 5.972e24, 0.0, 29780.0)) // earth
                add(Body(149.6e9 + 384400000, 0.0, 7.348e22, 0.0, 29780.0 + 1022)) // moon
            }
        }

        fun createSimpleSolarSystem(): Universe {
            return Universe(G = 1.0, dt = 0.001).apply {
                add(Body(0.0, 0.0, 1.0, 0.0, 0.0)) // sun
                add(Body(1.0, 0.0, 1e-4, 0.0, 1.0)) // earth
                add(Body(1.1, 0.0, 1e-8, 0.0, 1e-3 + 1.0)) // moon
            }
        }

        fun createDisc(): Universe {
            return Universe(G = 1.0, dt = 0.01).apply {
                val blackHole = Body(0.0, 0.0, 1.0, 0.0, 0.0)
                addRotatingDisc(10000, 1.0, 1e-9, blackHole, true)
            }
        }

        fun createCollidingDiscs(): Universe {
            val blackHole1 = Body(-1.5, -1.0, 10.0, 0.0, 0.0)
            val blackHole2 = Body(1.5, -1.0, 10.0, 0.0, 0.0)
            val blackHole3 = Body(0.0, 1.6, 10.0, 0.0, 0.0)
            return Universe(G = 0.01, dt = 0.01, theta = 0.7).apply {
                addRotatingDisc(1000, 1.2, 1e-4, blackHole1, false)
                addRotatingDisc(1000, 1.2, 1e-4, blackHole2, false)
                addRotatingDisc(1000, 1.2, 1e-4, blackHole3, false)
            }
        }

        fun createSpace(): Universe {
            val sigma = 0.2

            return Universe(G = 0.1, dt = 0.01).apply {
                repeat(10000) {
                    val rad = sqrt(Math.random()) * 100
                    val arc = Math.random() * 2 * Math.PI
                    val m = Math.random().pow(3)

                    val u1 = Math.random()
                    val u2 = Math.random()
                    val r = sqrt(-2.0 * ln(u1))
                    val theta = 2.0 * Math.PI * u2
                    val vx = r * cos(theta) * sigma
                    val vy = r * sin(theta) * sigma

                    add(
                        Body(
                            cos(arc) * rad, sin(arc) * rad,
                            m.pow(3),
                            vx, vy
                        )
                    )
                }
            }
        }

        fun createParticleSuckingCloud(): Universe {
            val distance = 100.0
            val massBlackHole = 1000.0
            val particleCount = 50000

            return Universe(G = 0.01, dt = 0.1).apply {
                add(Body(distance, 0.0, massBlackHole, 0.0, 0.3))

                val centerBlackHole = Body(0.0, 0.0, 100.0, 0.0, 0.0)
                addRotatingDisc(particleCount, 50.0, 1e-4, centerBlackHole, false)
            }
        }

        fun Universe.addRotatingDisc(count: Int, rad: Double, mass: Double, center: Body, isClockwise: Boolean) {
            repeat(count) {
                val r = sqrt(Math.random()) * rad
                val arc = Math.random() * 2 * PI
                val x = cos(arc) * r + center.x
                val y = sin(arc) * r + center.y
                val v = sqrt(G * center.mass / r)
                val vx = sin(arc) * v
                val vy = -cos(arc) * v

                if(!isClockwise) {
                    add(Body(x, y, mass, vx + center.vx, vy + center.vy))
                } else {
                    add(Body(x, y, mass, -vx + center.vx, -vy + center.vy))
                }
            }

            add(center)
        }
    }

    /*

    private fun createSpiral(): Universe {
        // initial velocity = sqrt(M / r)
        val centerMass = 1e7

        return Universe(G = 1.0, dt = 1.0).apply {
            addParticle(Particle(0.0, 0.0, centerMass, 0.0, 0.0)) // sun

            for(i in 2 .. 2000) {
                val r = i.toDouble() * 10
                val v = sqrt(centerMass / r)
                addParticle(Particle(r, 0.0, 0.001, 0.0, v))
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

                addParticle(Particle(x, y, mass, vx, vy))
            }

            addParticle(Particle(1e10, 0.0, massBlackHole, 0.0, 0.0))
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

                addParticle(Particle(x, y, mass, vx, vy))
            }

            addParticle(Particle(0.0, 0.0, massBlackHole, 0.0, 0.0))
        }
    }

    private fun createTwoRotatingBlackStars(): Universe {
        return Universe(G = 1.0, dt = 0.01).apply {
            val massBlackHole = 1e30
            val massSmallerBlackHole = 1e20
            val distanceBlackHoles = 1e8

            val v = sqrt(massBlackHole / distanceBlackHoles)

            addParticle(Particle(-distanceBlackHoles, 0.0, massSmallerBlackHole, 0.0, -v))
            addParticle(Particle(distanceBlackHoles, 0.0, massSmallerBlackHole, 0.0, v))
            addParticle(Particle(0.0, 0.0, massBlackHole, 0.0, 0.0))

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
     */
}