package searles.nbody2d

import kotlin.math.*

class BalancedBarnesHutTree {
    private var root: Node? = null

    val gx: Double get() = root?.gx ?: 0.0
    val gy: Double get() = root?.gy ?: 0.0

    fun updateForce(body: Body, G: Double, dt: Double, theta: Double) {
        body.resetTotalForce()
        root?.updateForce(body, theta, G, dt)
    }

    fun step(body: Body, dt: Double) {
        body.step(dt)

        if(body.parent?.containsInCorrectChild(body) == false) {
            relink(body)
        }
    }

    fun add(body: Body) {
        if(root == null) {
            root = body
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

        insert(root, body, x, y, len)
    }

    private fun relink(body: Body) {
        if(body == root) {
            root = null
            return
        }

        require(body.parent != null)

        val branch = body.parent!!
        val index = branch.children.indexOf(body)

        branch.children[index] = null
        body.parent = null

        val remainingChildren = branch.children.filterNotNull()

        require(remainingChildren.isNotEmpty())

        var parent = branch.parent

        if(remainingChildren.size == 1) {
            attach(parent, remainingChildren.first())
            // XXX In C or other languages delete "branch"
        }

        while(parent != null) {
            if(parent.contains(body.x, body.y)) {
                insert(parent, body, parent.x, parent.y, parent.len)
                return
            }

            parent = parent.parent
        }

        // was not inserted
        add(body)
    }

    private fun insert(node: Node?, body: Body, x: Double, y: Double, len: Double) {
        require(root != null)

        var node = node
        var x = x
        var y = y
        var len = len

        var parent: Branch? = null

        while(true) {
            require(isInside(body.x, body.y, x, y, len))

            if(node == null) {
                attach(parent, body)
                return
            }

            if(node is Body) {
                if(node.x == body.x && node.y == body.y) {
                    // XXX This is the proper place for a collision detection
                    // XXX In this case we assume that this practically never happens.
                    // XXX Alternative: Add masses
                    return
                }
            }

            if(node is Body || !(node as Branch).contains(body.x, body.y)) {
                val branch = Branch(x, y, len).apply {
                    shrinkForDistinctChildren(node!!.x, node!!.y, body.x, body.y)
                    attach(this, node!!)
                    attach(this, body)
                }

                attach(parent, branch)
                return
            }

            // XXX Rounding error if x == node.x and y == node.y.
            require(node.contains(body.x, body.y)) // node is Branch.

            len = node.len / 2
            x = if(body.x <= node.x) node.x - len else node.x + len
            y = if(body.y <= node.y) node.y - len else node.y + len

            parent = node
            node = node.children[node.indexOf(body.x, body.y)]
        }
    }

    private fun attach(parent: Branch?, node: Node) {
        node.parent = parent

        if (parent == null) {
            root = node
        } else {
            parent.children[parent.indexOf(node.x, node.y)] = node
        }
    }

    fun recalibrate() {
        root?.recalibrate()
    }

    companion object {
        fun isInside(x: Double, y: Double, x0: Double, y0: Double, len: Double): Boolean {
            return x0 - len <= x && x <= x0 + len
                    && y0 - len <= y && y <= y0 + len
        }
    }
}