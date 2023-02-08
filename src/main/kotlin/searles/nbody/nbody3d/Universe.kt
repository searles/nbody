package searles.nbody.nbody3d

import java.util.*
import kotlin.math.*

class Universe(val G: Double, val dt: Double = 1.0, val theta: Double = 0.7) {
    var generation: Int = 0
        private set
    var time: Double = 0.0
        private set

    private val tree = BarnesHutTree()

    var stats: Stats = tree.getStats()
        private set

    fun addAll(bodies: List<Particle>) {
        bodies.forEach {
            tree.add(it)
        }

        update()
    }

    fun add(body: Particle) {
        tree.add(body)
        update()
    }

    private fun update() {
        stats = tree.getStats()
    }

    suspend fun step() {
        tree.step(G, dt, theta)
        generation ++
        time = generation * dt
        update()
    }

    fun forEachParticle(action: (Particle) -> Unit) {
        tree.forEachParticle(action)
    }

    companion object {
        fun List<Particle>.withMotion(vx: Double, vy: Double, vz: Double): List<Particle> {
            this.forEach {
                it.vel.x += vx
                it.vel.y += vy
                it.vel.z += vz
            }

            return this
        }

        fun List<Particle>.moveBy(dx: Double, dy: Double, dz: Double): List<Particle> {
            this.forEach {
                it.pt.pos.x += dx
                it.pt.pos.y += dy
                it.pt.pos.z += dz
            }

            return this
        }

        fun createCloud(count: Int, rad: Double, maxMass: Double, sigmaV: Double): List<Particle> {
            return (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val polar = Math.random() * 2 * Math.PI
                val azimuth = Math.random() * 2 * Math.PI
                val m = Math.random() * maxMass

                val vr = sqrt(-2.0 * ln(Math.random())) * sigmaV
                val vPolar = 2.0 * Math.PI * Math.random()
                val vAzimuth = 2.0 * Math.PI * Math.random()

                Particle(
                    MassPoint(
                        Vec(
                            sin(polar) * cos(azimuth) * r,
                            sin(polar) * sin(azimuth) * r,
                            cos(polar) * r,
                        ),
                        m.pow(3)
                    ),
                    Vec(
                        sin(vPolar) * cos(vAzimuth) * vr,
                        sin(vPolar) * sin(vAzimuth) * vr,
                        cos(vPolar) * vr,
                    )
                )
            }
        }

        fun Universe.createRotatingCloud(count: Int, rad: Double, maxMass: Double, centerMass: Double): List<Particle> {
            return (0 until count).map {
                val r = Math.cbrt(Math.random()) * rad
                val polar = Math.random() * 2 * Math.PI
                val azimuth = Math.random() * 2 * Math.PI

                val x = sin(polar) * cos(azimuth) * r
                val y = sin(polar) * sin(azimuth) * r
                val z = cos(polar) * r

                val v = sqrt(G * centerMass / r)

                val alpha = Math.random() * 2 * Math.PI

                val vx = v * cos(azimuth) * sin(polar) * cos(alpha)
                val vy = v * sin(azimuth) * sin(polar) * cos(alpha)
                val vz = v * cos(polar) * cos(alpha)

                val m = Math.random() * maxMass

                Particle(MassPoint(Vec(x, y, z), m), Vec(vx, vy, vz))
            } + Particle(MassPoint(Vec(), centerMass), Vec())
        }

        fun Universe.createFlatRotatingDisc(
            count: Int,
            rad: Double,
            maxMass: Double,
            centerMass: Double,
            isClockwise: Boolean
        ): List<Particle> {
            return (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val arc = Math.random() * 2 * PI
                val x = cos(arc) * r
                val y = sin(arc) * r
                var v = sqrt(G * centerMass / r)

                if (isClockwise) v = -v

                val vx = sin(arc) * v
                val vy = -cos(arc) * v
                val m = Math.random() * maxMass

                Particle(MassPoint(Vec(x, y, 0.0), m), Vec(vx, vy, 0.0))
            } + Particle(MassPoint(Vec(), centerMass), Vec())
        }

        fun Universe.createRotatingBall(
            count: Int,
            rad: Double,
            maxMass: Double,
            centerMass: Double
        ): List<Particle> {
            return (0 until count).map {
                val r = rad // sqrt(Math.random()) * rad * 0.2 + 0.8 * rad
                val polar = Math.random() * Math.PI
                val azimuth = Math.random() * 2 * Math.PI

                val sp = sin(polar)
                val cp = cos(polar)
                val sa = sin(azimuth)
                val ca = cos(azimuth)

                val x = sp * ca * r
                val y = sp * sa * r
                val z = cp * r

                // polar rotates around the z-axis
                // azimuth rotates around y-axis

                val v = sqrt(G * centerMass / r)

                val t = Math.random() * 2 * Math.PI

                val ct = cos(t)
                val st = sin(t)

                // vector to be perpendicular is ct, st, 0

                val vx = v * (cp * ca * ct + (-sa) * st)
                val vy = v * (cp * sa * ct + ca * st)
                val vz = v * (-sp * ct)

                val m = Math.random() * maxMass

                Particle(MassPoint(Vec(x, y, z), m), Vec(vx, vy, vz))
            } + Particle(MassPoint(Vec(), centerMass), Vec())
        }


        fun createBall(
            count: Int,
            rad: Double,
            maxMass: Double,
        ): List<Particle> {
            val random = Random()

            return (0 until count).map {
                val r = sqrt(Math.random()) * rad * 0.2 + 0.8 * rad

                val u = random.nextGaussian()
                val v = random.nextGaussian()
                val w = random.nextGaussian()
                val norm = sqrt(u *u + v * v + w * w)

                val x = r * u / norm
                val y = r * v / norm
                val z = r * w / norm

                // polar rotates around the z-axis
                // azimuth rotates around y-axis

                val m = Math.random().pow(3) * maxMass

                Particle(MassPoint(Vec(x, y, z), m), Vec(0.0, 0.0, 0.0))
            }
        }

        fun createGalaxy(count: Int, rad: Double, maxMass: Double, velocity: Double): List<Particle> {
            return (0 until count).map {
                val theta = Math.random() * 2 * Math.PI
                val phi = acos(2 * Math.random() - 1)
                val x = rad * sin(phi) * cos(theta)
                val y = rad * sin(phi) * sin(theta)
                val z = rad * cos(phi)
                val vx = -velocity * sin(theta) * sin(phi)
                val vy = velocity * cos(theta) * sin(phi)
                val vz = 0.0
                val m = Math.random() * maxMass
                Particle(MassPoint(Vec(x, y, z), m), Vec(vx, vy, vz))
            }
        }
    }
}