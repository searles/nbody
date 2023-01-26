package searles

import kotlin.math.abs
import kotlin.math.max

// TODO what if two bodies have exactly same values?

class BalancedBarnesHutTree {
    var root: Node? = null
    val bodies = mutableListOf<Body>()

    fun check() {
        val bodyCount = root?.countBodies() ?: 0
        require(bodyCount == bodies.size)
    }

    fun add(body: Body) {
        if(root == null) {
            root = body
            bodies.add(body)
            return
        }

        var x: Double
        var y: Double
        var len: Double

        if(root is Body) {
            // 1 node in total
            val b = root as Body
            len = max(abs(body.x - b.x), abs(body.y - b.y)) * 1.00000000001 // makes rounding errors unlikely.
            x = (body.x + b.x) / 2
            y = (body.y + b.y) / 2
        } else {
            val branch = root as Branch

            x = branch.x
            y = branch.y
            len = branch.len

            // Expand if necessary
            while(!isInside(body.x, body.y, x, y, len)) {
                // opposite of 'shrink'
                if(body.x <= x) x -= len
                else x += len

                if(body.y <= y) y -= len
                else y += len

                len *= 2
            }
        }

        insert(body, x, y, len)
    }

    fun step(dt: Double) {
        bodies.forEach {
            step(it, dt)
        }
    }

    fun step(body: Body, dt: Double) {
        body.step(dt)

        if(body.parent?.contains(body.x, body.y) == false) {
            unlink(body)
            add(body)
        } else {
            body.parent?.recalibrate()
        }
    }

    fun unlink(body: Body) {
        if(body == root) {
            root = null
            return
        }

        require(body.parent != null)

        val branch = body.parent!!
        val index = branch.child.indexOf(body)

        branch.child[index] = null
        body.parent = null

        val remainingChildren = branch.child.filterNotNull()

        require(remainingChildren.isNotEmpty())

        if(remainingChildren.size == 1) {
            val parent = branch.parent
            parent.attach(remainingChildren.first())
            // XXX In C or other languages delete "branch"
            parent?.recalibrate()
        } else {
            branch.recalibrate()
        }
    }

    fun insert(body: Body, x: Double, y: Double, len: Double) {
        require(root != null)

        var x = x
        var y = y
        var len = len

        var node: Node? = root
        var parent: Branch? = null

        while(true) {
            require(isInside(body.x, body.y, x, y, len))

            if(node == null) {
                parent.attach(body)
                bodies.add(body)
                return
            }

            /*if(node is Body) {
                if(node.x == body.x && node.y == body.y) {
                    // XXX This is the proper place for a collision detection
                    return
                }
            }*/

            if(node is Body || !(node as Branch).contains(body.x, body.y)) {
                val branch = Branch(x, y, len).apply {
                    shrinkForDistinctChildren(node!!.x, node!!.y, body.x, body.y)
                    this.attach(node!!)
                    this.attach(body)
                }

                parent.attach(branch)
                bodies.add(body)
                return
            }

            // XXX Rounding error if x == node.x and y == node.y.
            require(node.contains(body.x, body.y))

            x = if(body.x <= node.x) node.x - node.len / 2 else node.x + node.len / 2
            y = if(body.y <= node.y) node.y - node.len / 2 else node.y + node.len / 2
            len = node.len / 2
            parent = node
            node = node.child[node.indexOf(body.x, body.y)]
        }
    }

    sealed class Node(var parent: Branch? = null, var x: Double, var y: Double) {
        abstract val gx: Double
        abstract val gy: Double
        abstract val mass: Double

        abstract fun countBodies(): Int
    }

    class Branch(
        x: Double, y: Double, var len: Double = 0.0,
        val child: Array<Node?> = Array(4) { null },
        parent: Branch? = null
    ): Node(parent, x, y) {
        override var gx: Double = 0.0
        override var gy: Double = 0.0
        override var mass: Double = 0.0

        fun contains(x: Double, y: Double): Boolean {
            return isInside(x, y, this.x, this.y, this.len)
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

                require(isInside(x0, y0, x, y, len)) { "($x0, $y0) is not inside ($x, $y; $len)" }
                require(isInside(x1, y1, x, y, len)) { "($x1, $y1) is not inside ($x, $y; $len)" }
            }
        }

        fun recalibrate() {
            var node: Branch? = this

            while(node != null) {
                node.mass = node.child.filterNotNull().sumOf { it.mass }
                node.gx = node.child.filterNotNull().sumOf { it.mass * it.gx } / node.mass
                node.gy = node.child.filterNotNull().sumOf { it.mass * it.gy } / node.mass

                node = node.parent
            }
        }

        override fun countBodies(): Int {
            require(child.filterNotNull().size >= 2)
            require(child.filterNotNull().all { this.contains(it.x, it.y) })
            require(child.filterNotNull().all { it.parent == this })

            return child.filterNotNull().sumOf { it.countBodies() }
        }
    }

    fun Branch?.attach(node: Node) {
        node.parent = this

        if (this == null) {
            root = node
        } else {
            this.child[indexOf(node.x, node.y)] = node
            this.recalibrate()
        }
    }

    class Body(
        x: Double,
        y: Double,
        override val mass: Double,
        var vx: Double,
        var vy: Double,
        parent: Branch? = null,
    ): Node(parent, x, y) {
        override val gx: Double get() = x
        override val gy: Double get() = y

        fun step(dt: Double) {
            x += vx * dt
            y += vy * dt
        }

        override fun countBodies(): Int {
            return 1
        }
    }

    companion object {
        fun isInside(x: Double, y: Double, x0: Double, y0: Double, len: Double): Boolean {
            return x0 - len <= x && x <= x0 + len
                    && y0 - len <= y && y <= y0 + len
        }
    }
}