package searles.nbody3d

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

sealed class Node(var parent: Branch? = null, var x: Double, var y: Double, var z: Double) {
    abstract val gx: Double
    abstract val gy: Double
    abstract val gz: Double
    abstract val mass: Double

    abstract fun updateForce(body: Body, theta: Double, G: Double, dt: Double)
    abstract fun recalibrate()
}

class Branch(
    x: Double, y: Double, z: Double, var len: Double,
    val children: Array<Node?> = Array(8) { null }
): Node(null, x, y, z) {
    override var gx: Double = 0.0
    override var gy: Double = 0.0
    override var gz: Double = 0.0
    override var mass: Double = 0.0

    fun contains(x: Double, y: Double, z: Double): Boolean {
        return BalancedBarnesHutTree.isInside(x, y, z, this.x, this.y, this.z, this.len)
    }

    fun containsInCorrectChild(body: Body): Boolean {
        if(!contains(body.x, body.y, body.z)) return false

        val index = children.indexOfFirst { it == body }
        require(index in 0..7)
        return indexOf(body.x, body.y, body.z) == index
    }

    fun indexOf(x: Double, y: Double, z: Double): Int {
        require(contains(x, y, z))

        return when {
            x <= this.x && y <= this.y && z <= this.z -> 0
            x <= this.x && y <= this.y && z > this.z -> 1
            x <= this.x && y > this.y && z <= this.z -> 2
            x <= this.x && y > this.y && z > this.z -> 3
            x > this.x && y <= this.y && z <= this.z -> 4
            x > this.x && y <= this.y && z > this.z -> 5
            x > this.x && y > this.y && z <= this.z -> 6
            x > this.x && y > this.y && z > this.z -> 7
            else -> error("no such case.")
        }
    }

    fun shrinkForDistinctChildren(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double) {
        while(true) {
            val index0 = indexOf(x0, y0, z0)
            val index1 = indexOf(x1, y1, z1)

            if(index0 != index1) {
                break
            }

            // There is a very small chance of rounding errors in the last double-digit.
            len /= 2

            when(index0) {
                0 -> { x -= len; y -= len; z -= len }
                1 -> { x -= len; y -= len; z += len }
                2 -> { x -= len; y += len; z -= len }
                3 -> { x -= len; y += len; z += len }
                4 -> { x += len; y -= len; z -= len }
                5 -> { x += len; y -= len; z += len }
                6 -> { x += len; y += len; z -= len }
                7 -> { x += len; y += len; z += len }
                else -> error("bad index")
            }
        }
    }

    override fun recalibrate() {
        var gxm = 0.0
        var gym = 0.0
        var gzm = 0.0
        var m = 0.0

        children.filterNotNull().forEach {
            it.recalibrate()

            gxm += it.gx * it.mass
            gym += it.gy * it.mass
            gzm += it.gz * it.mass
            m += it.mass
        }

        this.mass = m
        this.gx = gxm / m
        this.gy = gym / m
        this.gz = gzm / m
    }

    override fun updateForce(body: Body, theta: Double, G: Double, dt: Double) {
        // This uses recursion. We will need a path of length
        // log2(2 * bodies.size - 1) to store the current path.
        // Use 32, because 2^32 > 4 Bio.
        val distance = sqrt((gx - body.gx).pow(2) + (gy - body.gy).pow(2) + (gz - body.gz).pow(2)) + 1e-9
        if (2 * len / distance < theta) {
            body.addForce(this, G, dt)
        } else {
            for(it in children) {
                it?.updateForce(body, theta, G, dt)
            }
        }
    }
}

class Body(
    x: Double,
    y: Double,
    z: Double,
    override val mass: Double,
    var vx: Double,
    var vy: Double,
    var vz: Double
): Node(null, x, y, z) {
    override val gx: Double get() = x
    override val gy: Double get() = y
    override val gz: Double get() = z

    var totalForce: Double = 0.0

    fun resetTotalForce() {
        totalForce = 0.0
    }

    fun step(dt: Double) {
        x += vx * dt
        y += vy * dt
        z += vz * dt
    }

    override fun updateForce(body: Body, theta: Double, G: Double, dt: Double) {
        body.addForce(this, G, dt)
    }

    fun addForce(other: Node, G: Double, dt: Double) {
        if(other == this) return

        val distance = sqrt((gx - other.gx).pow(2) + (gy - other.gy).pow(2) + (gz - other.gz).pow(2)) + 1e-9
        val d0x = (other.gx - gx) / distance
        val d0y = (other.gy - gy) / distance
        val d0z = (other.gz - gz) / distance

        val force = (mass * other.mass * G) / distance.pow(2)
        val a = force / mass

        // the change of velocity should be smaller than d because it is into the direction of x2/y2.
        val ft = min(a * dt, 2 * distance)

        vx += d0x * ft
        vy += d0y * ft
        vz += d0z * ft

        totalForce += force // XXX or 'a'?
    }

    override fun recalibrate() {
        // nothing to do.
    }

    override fun toString(): String {
        return "Body(x = $gx, y = $gy, mass = $mass, vx = $vx, vy = $vy)"
    }
}