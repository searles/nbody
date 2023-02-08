package searles.nbody.nbody3d

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

class BarnesHutTree {
    private var root: Node? = null
    private val bodies = mutableListOf<Body>()

    fun getStats(): Stats {
        val cx = bodies.sumOf { it.particle.pt.pos.x } / bodies.size
        val cy = bodies.sumOf { it.particle.pt.pos.y } / bodies.size
        val cz = bodies.sumOf { it.particle.pt.pos.z } / bodies.size
        val s2x = bodies.sumOf { (it.particle.pt.pos.x - cx).pow(2) }
        val s2y = bodies.sumOf { (it.particle.pt.pos.y - cy).pow(2) }
        val s2z = bodies.sumOf { (it.particle.pt.pos.z - cz).pow(2) }

        val minMass = bodies.minOfOrNull { it.particle.pt.mass } ?: 0.0
        val maxMass = bodies.maxOfOrNull { it.particle.pt.mass } ?: 1.0

        var meanLogForce = 0.0
        var varianceLogForce = 0.0
        var n = 0

        forEachParticle {
            n++
            val logForce = ln(it.totalForce)

            varianceLogForce = (n - 1.0) / n * (varianceLogForce + meanLogForce.pow(2)) + logForce.pow(2) / n
            meanLogForce = (n - 1.0) / n * meanLogForce + logForce / n
            varianceLogForce -= meanLogForce.pow(2)
        }


        return Stats(cx, cy, cz, s2x, s2y, s2z, minMass, maxMass, meanLogForce, varianceLogForce)
    }

    fun forEachParticle(action: (Particle) -> Unit) {
        bodies.forEach { action(it.particle) }
    }

    fun add(particle: Particle) {
        val body = Body(particle)
        bodies.add(body)
        insert(body)
    }

    suspend fun step(G: Double, dt: Double, theta: Double) {
        // run in parallel
        val chunkSize = 500
        val chunks = bodies.chunked(chunkSize)

        root?.recalibrate()
        bodies.forEach { updateForce(it.particle, G, theta) }
//        coroutineScope {
//            root?.recalibrate()
//
//            val jobs = chunks.map { chunk ->
//                async {
//                    chunk.forEach { updateForce(it.particle, G, theta) }
//                }
//            }
//
//            jobs.awaitAll()
//        }

        for(body in bodies) {
            move(body, dt) // not in parallel because it modifies the tree.
        }
    }

    private fun updateForce(particle: Particle, G: Double, theta: Double) {
        particle.resetTotalForce()
        root?.updateForceForParticle(particle, theta, G)
    }

    private fun move(body: Body, dt: Double) {
        body.particle.move(dt)

        if(body.parent?.containsInCorrectChild(body) == false) {
            unlink(body, true)
        }
    }

    private fun insert(body: Body) {
        if(root == null) {
            root = body
            return
        }

        val center = Vec()
        var len: Double

        if(root is Body) {
            // 1 node in total
            val b = root as Body
            center.setToMiddle(body.particle.pt.pos, b.particle.pt.pos)
            len = body.particle.pt.pos.manhattanDistance(b.particle.pt.pos) * 1.00000000001 // makes rounding errors unlikely.
        } else {
            val branch = root as Branch

            center.setTo(branch.center)
            len = branch.len

            // Expand if necessary
            while(!isInside(body.particle.pt.pos, center, len)) {
                // opposite of 'shrink'
                if(body.particle.pt.pos.x <= center.x) center.x -= len
                else center.x += len

                if(body.particle.pt.pos.y <= center.y) center.y -= len
                else center.y += len

                if(body.particle.pt.pos.z <= center.z) center.z -= len
                else center.z += len

                len *= 2
            }
        }

        insert(root, body, center, len)
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
            if(parent.contains(body.particle.pt.pos)) {
                insert(parent, body, parent.center.copy(), parent.len)
                return
            }

            parent = parent.parent
        }

        // was not inserted, maybe we need a new root.
        insert(body)
    }

    private fun insert(node: Node?, body: Body, center: Vec, len: Double) {
        // node = current node in iteration. body is inserted at the current position of node or below it.
        var node = node
        var len = len

        require(root != null)

        var parent: Branch? = null

        while(true) {
            require(node == null || isInside(node.center, center, len))
            require(isInside(body.particle.pt.pos, center, len))

            if(node == null) {
                attach(parent, body)
                return
            }

            // XXX This is the proper place for a collision detection
            // XXX In this case we assume that this practically never happens.

            if(node is Body || !(node as Branch).contains(body.particle.pt.pos)) {
                val branch = Branch(center, len).apply {
                    shrinkForDistinctChildren(node!!.center, body.particle.pt.pos)
                    attach(this, node!!)
                    attach(this, body)
                }

                attach(parent, branch)
                return
            }

            // XXX Rounding error if x == node.x and y == node.y.
            require(node.contains(body.particle.pt.pos)) // node is Branch.

            len = node.len / 2
            center.x = if(body.particle.pt.pos.x <= node.center.x) node.center.x - len else node.center.x + len
            center.y = if(body.particle.pt.pos.y <= node.center.y) node.center.y - len else node.center.y + len
            center.z = if(body.particle.pt.pos.z <= node.center.z) node.center.z - len else node.center.z + len

            parent = node
            node = node.children[node.indexOf(body.particle.pt.pos)]
        }
    }

    private fun attach(parent: Branch?, node: Node) {
        node.parent = parent

        if (parent == null) {
            root = node
        } else {
            parent.children[parent.indexOf(node.center)] = node
        }
    }

    companion object {
        fun isInside(pt: Vec, center: Vec, len: Double): Boolean {
            return pt.manhattanDistance(center) <= len
        }
    }
}

data class Stats(
    val cx: Double, val cy: Double, val cz: Double,
    val s2x: Double, val s2y: Double, val s2z: Double,
    val minMass: Double, val maxMass: Double,
    val meanLogForce: Double, val varianceLogForce: Double,
)