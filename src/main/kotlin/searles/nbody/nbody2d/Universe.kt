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

        fun createCollidingDiscs(): Universe {
            val blackHole1 = Body(-3.0, 0.0, 20.0, 0.05, -0.0866)
            val blackHole2 = Body(3.0, 0.0, 20.0, -0.05, 0.0866)
            //val blackHole3 = Body(0.0, 1.6, 10.0, -0.1, 0.0)
            return Universe(G = 0.01, dt = 0.01, theta = 0.7).apply {
                addAll(createRotatingDisc(20000, 1.5, 1e-4, blackHole1, true))
                addAll(createRotatingDisc(20000, 1.5, 1e-4, blackHole2, true))
                //addRotatingDisc(10000, 1.2, 1e-4, blackHole3, true)
            }
        }

        fun Universe.particleSuckingCloud(
            count: Int,
            massBlackHole: Double,
            distance: Double,
            massCenter: Double,
            rad: Double
        ): List<Body> {
            val massiveBlackHole = Body(distance, 0.0, massBlackHole, 0.0, 0.3)
            val centerBlackHole = Body(0.0, 0.0, massCenter, 0.0, 0.0)

            return listOf(massiveBlackHole) +
                    createRotatingDisc(count, rad, 1e-4, centerBlackHole, false)
        }

        fun Universe.createCloud(count: Int, rad: Double = 100.0, sigmaV: Double = 0.5): List<Body> {
            return (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val arc = Math.random() * 2 * Math.PI
                val m = Math.cbrt(Math.random()) * 0.14

                val u1 = Math.random()
                val u2 = Math.random()
                val rV = sqrt(-2.0 * ln(u1))
                val theta = 2.0 * Math.PI * u2
                val vx = rV * cos(theta) * sigmaV
                val vy = rV * sin(theta) * sigmaV

                Body(cos(arc) * r, sin(arc) * r, m.pow(3), vx, vy)
            }
        }

        fun Universe.createRotatingDisc(
            count: Int,
            rad: Double,
            mass: Double,
            center: Body,
            isClockwise: Boolean
        ): List<Body> {
            return listOf(center) + (0 until count).map {
                val r = sqrt(Math.random()) * rad
                val arc = Math.random() * 2 * PI
                val x = cos(arc) * r + center.x
                val y = sin(arc) * r + center.y
                var v = sqrt(G * center.mass / r)

                if (isClockwise) v = -v

                val vx = sin(arc) * v
                val vy = -cos(arc) * v
                val m = Math.random() * mass

                Body(x, y, m, vx + center.vx, vy + center.vy)
            }
        }
    }
}