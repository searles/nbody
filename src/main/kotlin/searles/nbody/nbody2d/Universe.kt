package searles.nbody.nbody2d

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
        fun List<Body>.withMotion(vx: Double, vy: Double): List<Body> {
            this.forEach {
                it.vx += vx
                it.vy += vy
            }

            return this
        }

        fun List<Body>.moveBy(dx: Double, dy: Double): List<Body> {
            this.forEach {
                it.x += dx
                it.y += dy
            }

            return this
        }

        fun createCloud(count: Int, rad: Double, maxMass: Double, sigmaV: Double): List<Body> {
            return (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val arc = Math.random() * 2 * Math.PI
                val m = Math.random() * maxMass

                val u1 = Math.random()
                val u2 = Math.random()
                val rV = sqrt(-2.0 * ln(u1))
                val theta = 2.0 * Math.PI * u2
                val vx = rV * cos(theta) * sigmaV
                val vy = rV * sin(theta) * sigmaV

                Body(cos(arc) * r, sin(arc) * r, m, vx, vy)
            }
        }

        fun Universe.createRotatingDisc(
            count: Int,
            rad: Double,
            maxMass: Double,
            centerMass: Double,
            isClockwise: Boolean
        ): List<Body> {
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

                Body(x, y, m, vx, vy)
            } + Body(0.0, 0.0, centerMass, 0.0, 0.0)
        }
    }
}