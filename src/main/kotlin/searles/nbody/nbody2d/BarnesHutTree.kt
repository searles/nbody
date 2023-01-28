package searles.nbody.nbody2d

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
        val s2x = bodies.sumOf { (it.x - cx).pow(2) }
        val s2y = bodies.sumOf { (it.y - cy).pow(2) }

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


        return BodyStats(cx, cy, s2x, s2y, minMass, maxMass, meanLogForce, varianceLogForce)
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
            relink(body)
        }
    }

    private fun insert(body: Body) {
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

        // was not inserted, maybe we need a new root.
        insert(body)
    }

    private fun insert(node: Node?, body: Body, x: Double, y: Double, len: Double) {
        var node = node
        var x = x
        var y = y
        var len = len

        require(root != null)

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

    companion object {
        fun isInside(x: Double, y: Double, x0: Double, y0: Double, len: Double): Boolean {
            return x0 - len <= x && x <= x0 + len
                    && y0 - len <= y && y <= y0 + len
        }
    }
}

data class BodyStats(
    val cx: Double, val cy: Double,
    val s2x: Double, val s2y: Double,
    val minMass: Double, val maxMass: Double,
    val meanLogForce: Double, val varianceLogForce: Double,
)