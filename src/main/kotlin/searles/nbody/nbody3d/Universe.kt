package searles.nbody.nbody3d

import kotlin.math.*

class Universe(val G: Double, val dt: Double = 1.0, val theta: Double = 0.7) {
    var generation: Int = 0
        private set
    var time: Double = 0.0
        private set

    private val tree = BarnesHutTree()

    var bodyStats: BodyStats = tree.getBodyStats()
        private set

    fun addAll(bodies: List<Body>) {
        bodies.forEach {
            tree.add(it)
        }

        update()
    }

    private fun update() {
        bodyStats = tree.getBodyStats()
    }

    suspend fun step() {
        tree.step(G, dt, theta)
        generation ++
        time = generation * dt
        update()
    }

    fun forEachBody(action: (Body) -> Unit) {
        tree.forEachBody(action)
    }

    companion object {
        fun List<Body>.withMotion(vx: Double, vy: Double, vz: Double): List<Body> {
            this.forEach {
                it.vx += vx
                it.vy += vy
                it.vz += vz
            }

            return this
        }

        fun List<Body>.moveBy(dx: Double, dy: Double, dz: Double): List<Body> {
            this.forEach {
                it.x += dx
                it.y += dy
                it.z += dz
            }

            return this
        }

        fun createCloud(count: Int, rad: Double, maxMass: Double, sigmaV: Double): List<Body> {
            return (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val polar = Math.random() * 2 * Math.PI
                val azimuth = Math.random() * 2 * Math.PI
                val m = Math.random() * maxMass

                val vr = sqrt(-2.0 * ln(Math.random())) * sigmaV
                val vPolar = 2.0 * Math.PI * Math.random()
                val vAzimuth = 2.0 * Math.PI * Math.random()

                Body(
                    sin(polar) * cos(azimuth) * r,
                    sin(polar) * sin(azimuth) * r,
                    cos(polar) * r,
                    m.pow(3),
                    sin(vPolar) * cos(vAzimuth) * vr,
                    sin(vPolar) * sin(vAzimuth) * vr,
                    cos(vPolar) * vr,
                )
            }
        }

        fun Universe.createRotatingCloud(count: Int, rad: Double, maxMass: Double, centerMass: Double): List<Body> {
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

                Body(x, y, z, m, vx, vy, vz)
            } + Body(0.0, 0.0, 0.0, centerMass, 0.0, 0.0, 0.0)
        }

        fun createGalaxy(count: Int, rad: Double, maxMass: Double, velocity: Double): List<Body> {
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
                Body(x, y, z, m, vx, vy, vz)
            }
        }
    }
}