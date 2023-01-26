package searles

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow

sealed class Node(var parent: Branch? = null, var x: Double, var y: Double) {
    abstract val gx: Double
    abstract val gy: Double
    abstract val mass: Double

    abstract fun updateForce(body: Body, theta: Double, G: Double, dt: Double)
    abstract fun validate()
}

class Branch(
    x: Double, y: Double, var len: Double,
    val children: Array<Node?> = Array(4) { null }
): Node(null, x, y) {
    override var gx: Double = 0.0
    override var gy: Double = 0.0
    override var mass: Double = 0.0

    fun contains(x: Double, y: Double): Boolean {
        return BalancedBarnesHutTree.isInside(x, y, this.x, this.y, this.len)
    }

    fun containsInCorrectChild(body: Body): Boolean {
        if(!contains(body.x, body.y)) return false

        val index = children.indexOfFirst { it == body }
        require(index in 0..3)
        return indexOf(body.x, body.y) == index
    }

    fun indexOf(x: Double, y: Double): Int {
        require(contains(x, y))

        return when {
            x <= this.x && y <= this.y -> 0
            x > this.x && y <= this.y -> 1
            x > this.x && y > this.y -> 2
            else -> 3
        }
    }

    fun shrinkForDistinctChildren(x0: Double, y0: Double, x1: Double, y1: Double) {
        validate()
        while(true) {
            val index0 = indexOf(x0, y0)
            val index1 = indexOf(x1, y1)

            if(index0 != index1) {
                break
            }

            // There is a very small chance of rounding errors in the last double-digit.
            len /= 2

            when(index0) {
                0 -> { x -= len; y -= len }
                1 -> { x += len; y -= len }
                2 -> { x += len; y += len }
                else -> { x -= len; y += len }
            }

//            require(BalancedBarnesHutTree.isInside(x0, y0, x, y, len)) { "($x0, $y0) is not inside (${x-len} - ${x+len}, ${y-len} - ${y+len})" }
//            require(BalancedBarnesHutTree.isInside(x1, y1, x, y, len)) { "($x1, $y1) is not inside (${x-len} - ${x+len}, ${y-len} - ${y+len})" }
        }
    }

    fun recalibrate() {
        var node: Branch? = this

        while(node != null) {
            node.mass = node.children.filterNotNull().sumOf { it.mass }
            node.gx = node.children.filterNotNull().sumOf { it.mass * it.gx } / node.mass
            node.gy = node.children.filterNotNull().sumOf { it.mass * it.gy } / node.mass

            node = node.parent
        }
    }

    override fun updateForce(body: Body, theta: Double, G: Double, dt: Double) {
        // This uses recursion. We will need a path of length
        // log2(2 * bodies.size - 1) to store the current path.
        // Use 32, because 2^32 > 4 Bio.
        val distance = hypot(body.gx - gx, body.gy - gy)
        if (2 * len / distance < theta) {
            body.addForce(this, G, dt)
        } else {
            for(it in children) {
                it?.updateForce(body, theta, G, dt)
            }
        }
    }

    override fun validate() {
        require(this.parent == null || this.parent!!.len > this.len)
        children.filterNotNull().forEach {
            require(this.contains(it.x, it.y))
            it.validate()
        }
    }
}

class Body(
    x: Double,
    y: Double,
    override val mass: Double,
    var vx: Double,
    var vy: Double
): Node(null, x, y) {
    override val gx: Double get() = x
    override val gy: Double get() = y

    var totalForce: Double = 0.0

    fun resetTotalForce() {
        totalForce = 0.0
    }

    fun step(dt: Double) {
        x += vx * dt
        y += vy * dt
    }

    override fun updateForce(body: Body, theta: Double, G: Double, dt: Double) {
        body.addForce(this, G, dt)
    }

    override fun validate() {}

    fun addForce(other: Node, G: Double, dt: Double) {
        if(other == this) return

        val distance = hypot(gx - other.gx, gy - other.gy) + 1e-9
        val d0x = (other.gx - gx) / distance
        val d0y = (other.gy - gy) / distance

        val force = (mass * other.mass * G) / distance.pow(2)
        val a = force / mass

        // the change of velocity should be smaller than d because it is into the direction of x2/y2.
        val ft = min(a * dt, 2 * distance)

        vx += d0x * ft
        vy += d0y * ft
        totalForce += force // XXX or 'a'?
    }

    override fun toString(): String {
        return "Body(x = $gx, y = $gy, mass = $mass, vx = $vx, vy = $vy)"
    }
}