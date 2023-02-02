package searles.nbody.nbody3d

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

class Universe(val G: Double, val dt: Double = 1.0, val theta: Double = 0.7) {
    val centerX: Double get() = tree.gx
    val centerY: Double get() = tree.gy
    val centerZ: Double get() = tree.gz

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
            tree.recalibrate()

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
        var s2z = 0.0

        bodies.forEach {
            s2x += (it.gx - centerX).pow(2)
            s2y += (it.gy - centerY).pow(2)
            s2z += (it.gy - centerY).pow(2)
        }

        s2x /= bodies.size - 1
        s2y /= bodies.size - 1
        s2z /= bodies.size - 1

        return sqrt(max(max(s2x, s2y), s2z))
    }

    fun forEachBody(action: (Body) -> Unit) {
        for(p in bodies) {
            action(p)
        }
    }

    companion object {
        fun createDisc(): Universe {
            return Universe(G = 1.0, dt = 0.01).apply {
                val blackHole = Body(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)
                addRotatingDisc(10000, 1.0, 1e-9, blackHole, true)
            }
        }

        fun createCollidingDiscs(): Universe {
            val blackHole1 = Body(-1.5, -1.0, 0.0, 10.0, 0.0, 0.0, 0.0)
            val blackHole2 = Body(1.5, -1.0, 0.0, 10.0, 0.0, 0.0, 0.0)
            val blackHole3 = Body(0.0, 1.6, 0.0, 10.0, 0.0, 0.0, 0.0)
            return Universe(G = 0.01, dt = 0.01, theta = 0.7).apply {
                addRotatingDisc(4000, 1.2, 1e-4, blackHole1, false)
                addRotatingDisc(4000, 1.2, 1e-4, blackHole2, false)
                addRotatingDisc(4000, 1.2, 1e-4, blackHole3, false)
            }
        }

        fun createSimpleSolarSystem(): Universe {
            return Universe(G = 1.0, dt = 0.01).apply {
                add(Body(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)) // sun
                add(Body(1.0, 0.0, 0.0, 1e-4, 0.0, 1.0, 0.0)) // earth
                add(Body(1.1, 0.0, 0.0, 1e-8, 0.0, 1e-3 + 1.0, 0.0)) // moon
            }
        }

        fun createSpace(): Universe {
            val sigma = 0.2

            return Universe(G = 0.1, dt = 0.01).apply {
                repeat(12) {
                    val rad = sqrt(Math.random()) * 10
                    val polar = Math.random() * 2 * Math.PI
                    val azimuth = Math.random() * 2 * Math.PI
                    val m = Math.cbrt(Math.random())

                    val u1 = Math.random()
                    val u2 = Math.random()
                    val vRad = sqrt(-2.0 * ln(u1))
                    val vPolar = 2.0 * Math.PI * u2
                    val vAzimuth = 2.0 * Math.PI * u2

                    add(
                        Body(
                            sin(polar) * cos(azimuth) * rad,
                            sin(polar) * sin(azimuth) * rad,
                            cos(polar) * rad,
                            m.pow(3),
                            sin(vPolar) * cos(vAzimuth) * vRad,
                            sin(vPolar) * sin(vAzimuth) * vRad,
                            cos(vPolar) * vRad,
                        )
                    )
                }
            }
        }
    }
//
//        fun createParticleSuckingCloud(): Universe {
//            val distance = 100.0
//            val massBlackHole = 1000.0
//            val particleCount = 100000
//
//            return Universe(G = 0.01, dt = 0.1).apply {
//                add(Body(distance, 0.0, massBlackHole, 0.0, 0.3))
//
//                val centerBlackHole = Body(0.0, 0.0, 100.0, 0.0, 0.0)
//                addRotatingDisc(particleCount, 50.0, 1e-4, centerBlackHole, false)
//            }
//        }
//
        fun addRotatingDisc(count: Int, rad: Double, mass: Double, center: Body, isClockwise: Boolean) {
            repeat(count) {
                val r = sqrt(Math.random()) * rad
                val polar = Math.random() * 2 * PI
                val azimuth = Math.random() * 2 * PI
                val x = sin(polar) * cos(azimuth) * r + center.x
                val y = sin(polar) * sin(azimuth) * r + center.y
                val z = cos(polar) * r + center.z
                val v = sqrt(G * center.mass / r)
                val vx = sin(polar) * sin(azimuth) * v
                val vy = -sin(polar) * cos(azimuth) * v
                val vz = 0.0

                if(!isClockwise) {
                    add(Body(x, y, z, mass, vx + center.vx, vy + center.vy, vz + center.vz))
                } else {
                    add(Body(x, y, z, mass, -vx + center.vx, -vy + center.vy, -vz + center.vz))
                }
            }

            add(center)
        }
//    }

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