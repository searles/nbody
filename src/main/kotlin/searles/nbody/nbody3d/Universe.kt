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
                val r = sqrt(Math.random()) * 10
                val polar = Math.random() * 2 * Math.PI
                val azimuth = Math.random() * 2 * Math.PI
                val m = Math.cbrt(Math.random())

                val vRad = sqrt(-2.0 * ln(Math.random()))
                val vPolar = 2.0 * Math.PI * Math.random()
                val vAzimuth = 2.0 * Math.PI * Math.random()

                Body(
                    sin(polar) * cos(azimuth) * rad,
                    sin(polar) * sin(azimuth) * rad,
                    cos(polar) * rad,
                    m.pow(3),
                    sin(vPolar) * cos(vAzimuth) * vRad,
                    sin(vPolar) * sin(vAzimuth) * vRad,
                    cos(vPolar) * vRad,
                )
            }
        }
    }
}