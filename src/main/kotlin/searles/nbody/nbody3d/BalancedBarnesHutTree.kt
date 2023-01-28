package searles.nbody.nbody3d

import kotlin.math.*

class BalancedBarnesHutTree {
    private var root: Node? = null

    val gx: Double get() = root?.gx ?: 0.0
    val gy: Double get() = root?.gy ?: 0.0
    val gz: Double get() = root?.gz ?: 0.0

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
        var z: Double
        var len: Double

        if(root is Body) {
            // 1 node in total
            val b = root as Body
            len = max(max(abs(body.x - b.x), abs(body.y - b.y)), abs(body.z - b.z)) * 1.00000000001 // makes rounding errors unlikely.
            x = (body.x + b.x) / 2
            y = (body.y + b.y) / 2
            z = (body.z + b.z) / 2
        } else {
            val branch = root as Branch

            x = branch.x
            y = branch.y
            z = branch.z
            len = branch.len

            // Expand if necessary
            while(!isInside(body.x, body.y, body.z, x, y, z, len)) {
                // opposite of 'shrink'
                if(body.x <= x) x -= len
                else x += len

                if(body.y <= y) y -= len
                else y += len

                if(body.z <= z) z -= len
                else z += len

                len *= 2
            }
        }

        insert(root, body, x, y, z, len)
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
            if(parent.contains(body.x, body.y, body.z)) {
                insert(parent, body, parent.x, parent.y, parent.z, parent.len)
                return
            }

            parent = parent.parent
        }

        // was not inserted
        add(body)
    }

    private fun insert(node: Node?, body: Body, x: Double, y: Double, z: Double, len: Double) {
        require(root != null)

        var node = node
        var x = x
        var y = y
        var z = z
        var len = len

        var parent: Branch? = null

        while(true) {
            require(isInside(body.x, body.y, body.z, x, y, z, len))

            if(node == null) {
                attach(parent, body)
                return
            }

            if(node is Body) {
                if(node.x == body.x && node.y == body.y && node.z == body.z) {
                    // XXX This is the proper place for a collision detection
                    // XXX In this case we assume that this practically never happens.
                    // XXX Alternative: Add masses
                    return
                }
            }

            if(node is Body || !(node as Branch).contains(body.x, body.y, body.z)) {
                val branch = Branch(x, y, z, len).apply {
                    shrinkForDistinctChildren(node!!.x, node!!.y, node!!.z, body.x, body.y, body.z)
                    attach(this, node!!)
                    attach(this, body)
                }

                attach(parent, branch)
                return
            }

            // XXX Rounding error if x == node.x and y == node.y.
            require(node.contains(body.x, body.y, body.z)) // node is Branch.

            len = node.len / 2
            x = if(body.x <= node.x) node.x - len else node.x + len
            y = if(body.y <= node.y) node.y - len else node.y + len
            z = if(body.z <= node.z) node.z - len else node.z + len

            parent = node
            node = node.children[node.indexOf(body.x, body.y, body.z)]
        }
    }

    private fun attach(parent: Branch?, node: Node) {
        node.parent = parent

        if (parent == null) {
            root = node
        } else {
            parent.children[parent.indexOf(node.x, node.y, node.z)] = node
        }
    }

    fun recalibrate() {
        root?.recalibrate()
    }

    companion object {
        fun isInside(x: Double, y: Double, z: Double, x0: Double, y0: Double, z0: Double, len: Double): Boolean {
            return x0 - len <= x && x <= x0 + len
                    && y0 - len <= y && y <= y0 + len
                    && z0 - len <= z && z <= z0 + len
        }
    }
}