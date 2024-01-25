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

    fun step(G: Double, dt: Double, theta: Double) {
        // run in parallel
        val chunkSize = 500
        val chunks = bodies.chunked(chunkSize)

        root?.recalibrate()
        bodies.forEach { updateForce(it.particle, G, theta) } // TODO
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

        // TODO check collisions.

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

        val region: Region = if(root is Body) {
            // 1 node in total
            val b = root as Body

            Region(
                Vec().apply { setToMiddle(body.center, b.center) },
                body.center.manhattanDistance(b.center) * 1.5
            )
        } else {
            val branch = root as Branch

            Region(
                Vec().apply { setTo(branch.region.center) },
                branch.region.len
            ).apply {
                enlargeTowards(body.center)
            }
        }

        insert(root, body, region)
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
            if(parent.contains(body.center)) {
                insert(parent, body, parent.region.copy(center = parent.region.center.copy()))
                return
            }

            parent = parent.parent
        }

        // was not inserted, maybe we need a new root.
        insert(body)
    }

    private fun insert(node: Node?, body: Body, region: Region) {
        // node = current node in iteration. body is inserted at the current position of node or below it.
        var node = node

        require(root != null)

        var parent: Branch? = null

        while(true) {
            if(!(node == null || region.isInside(node.center))) {
                println("fail")
            }

            require(node == null || region.isInside(node.center))
            require(region.isInside(body.center))

            if(node == null) {
                attach(parent, body)
                return
            }

            // XXX This is the proper place for a collision detection
            // XXX In this case we assume that this practically never happens.

            if(node is Body || !(node as Branch).contains(body.center)) {
                region.shrinkForDistinctChildren(node.center, body.center)
                val branch = Branch(region).apply {
                    attach(this, node!!)
                    attach(this, body)
                }

                attach(parent, branch)
                return
            }

            require(node.contains(body.center)) // node is Branch.

// FIXME compare to next 2 lines.           len = node.len / 2
//            center.x = if(body.particle.pt.pos.x <= node.center.x) node.center.x - len else node.center.x + len
//            center.y = if(body.particle.pt.pos.y <= node.center.y) node.center.y - len else node.center.y + len
//            center.z = if(body.particle.pt.pos.z <= node.center.z) node.center.z - len else node.center.z + len

            // XXX Rounding error if x == node.x and y == node.y.
            region.setTo(node.region)
            region.shrink(body.center)

            parent = node
            node = node.children[node.indexOf(body.center)]
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
}

data class Stats(
    val cx: Double, val cy: Double, val cz: Double,
    val s2x: Double, val s2y: Double, val s2z: Double,
    val minMass: Double, val maxMass: Double,
    val meanLogForce: Double, val varianceLogForce: Double,
)