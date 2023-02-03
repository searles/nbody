package searles.nbody.nbody3d

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

class BarnesHutTree {
    private var root: Node? = null
    private val bodies = mutableListOf<Body>()

    fun getBodyStats(): BodyStats {
        val cx = bodies.sumOf { it.x } / bodies.size
        val cy = bodies.sumOf { it.y } / bodies.size
        val cz = bodies.sumOf { it.z } / bodies.size
        val s2x = bodies.sumOf { (it.x - cx).pow(2) }
        val s2y = bodies.sumOf { (it.y - cy).pow(2) }
        val s2z = bodies.sumOf { (it.z - cz).pow(2) }

        val minMass = bodies.minOfOrNull { it.mass } ?: 0.0
        val maxMass = bodies.maxOfOrNull { it.mass } ?: 1.0

        var meanLogForce = 0.0
        var varianceLogForce = 0.0
        var n = 0

        forEachBody {
            n++
            val logForce = ln(it.totalForce)

            varianceLogForce = (n - 1.0) / n * (varianceLogForce + meanLogForce.pow(2)) + logForce.pow(2) / n
            meanLogForce = (n - 1.0) / n * meanLogForce + logForce / n
            varianceLogForce -= meanLogForce.pow(2)
        }


        return BodyStats(cx, cy, cz, s2x, s2y, s2z, minMass, maxMass, meanLogForce, varianceLogForce)
    }

    fun forEachBody(action: (Body) -> Unit) {
        bodies.forEach { action(it) }
    }

    fun add(body: Body) {
        bodies.add(body)
        insert(body)
    }

    suspend fun step(G: Double, dt: Double, theta: Double) {
        // run in parallel
        val chunkSize = 500
        val chunks = bodies.chunked(chunkSize)

        coroutineScope {
            root?.recalibrate()

            val jobs = chunks.map { chunk ->
                async {
                    chunk.forEach { updateForce(it, G, dt, theta) }
                }
            }

            jobs.awaitAll()
        }

        for(body in bodies) {
            move(body, dt) // not in parallel because it modifies the tree.
        }
    }

    private fun updateForce(body: Body, G: Double, dt: Double, theta: Double) {
        body.resetTotalForce()
        root?.updateForceForBody(body, theta, G, dt)
    }

    private fun move(body: Body, dt: Double) {
        body.move(dt)

        if(body.parent?.containsInCorrectChild(body) == false) {
            unlink(body, true)
        }
    }

    private fun insert(body: Body) {
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

    private fun unlink(body: Body, reinsert: Boolean) {
        if(body == root) {
            // This is in fact impossible.
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

        if(!reinsert) {
            return
        }

        while(parent != null) {
            if(parent.contains(body.x, body.y, body.z)) {
                insert(parent, body, parent.x, parent.y, parent.z, parent.len)
                return
            }

            parent = parent.parent
        }

        // was not inserted, maybe we need a new root.
        insert(body)
    }

    private fun insert(node: Node?, body: Body, x: Double, y: Double, z: Double, len: Double) {
        var node = node
        var x = x
        var y = y
        var z = z
        var len = len

        require(root != null)

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

    companion object {
        fun isInside(x: Double, y: Double, z: Double, x0: Double, y0: Double, z0: Double, len: Double): Boolean {
            return x0 - len <= x && x <= x0 + len
                    && y0 - len <= y && y <= y0 + len
                    && z0 - len <= z && z <= z0 + len
        }
    }
}

data class BodyStats(
    val cx: Double, val cy: Double, val cz: Double,
    val s2x: Double, val s2y: Double, val s2z: Double,
    val minMass: Double, val maxMass: Double,
    val meanLogForce: Double, val varianceLogForce: Double,
)