package searles

import kotlin.math.hypot

class BarnesHutTree(x0: Double, y0: Double, len: Double) {
    var root: Node = ExternalNode(x0, y0, len)

    fun insert(p: Particle) {
        while(!root.isInside(p)) {
            expand(p.x, p.y)
        }
    }

    fun step(G: Double, dt: Double, theta: Double) {
        root.forAllParticles {
            root.updateForce(it, theta, G, dt)
        }
    }

    private fun expand(x: Double, y: Double) {
        root = if(x < root.x0) {
            if(y < root.y0) {
                InnerNode(root.x0 - root.len, root.y0 - root.len, 2 * root.len, leftTop = root)
            } else {
                InnerNode(root.x0 - root.len, root.y0, 2 * root.len, leftBottom = root)
            }
        } else {
            if(y < root.y0) {
                InnerNode(root.x0, root.y0 - root.len, 2 * root.len, rightTop = root)
            } else {
                InnerNode(root.x0, root.y0, 2 * root.len, rightBottom = root)
            }
        }
    }
}

abstract class Node(val x0: Double, val y0: Double, val len: Double) {
    abstract val x: Double
    abstract val y: Double
    abstract val m: Double

    fun isInside(p: Particle): Boolean {
        return (p.x in x0 .. x0 + len) && (p.y in y0 .. y0 + len)
    }

    abstract fun insert(p: Particle): Node

    abstract fun step(dt: Double, removedParticles: MutableList<Particle>): Node

    abstract fun updateForce(p: Particle, theta: Double, G: Double, dt: Double)

    abstract fun forAllParticles(action: (Particle) -> Unit)
}

class InnerNode(x0: Double, y0: Double, len: Double,
    leftTop: Node = ExternalNode(x0, y0, len / 2),
    rightTop: Node = ExternalNode(x0 + len / 2, y0, len / 2),
    rightBottom: Node = ExternalNode(x0 + len / 2, y0 + len / 2, len / 2),
    leftBottom: Node = ExternalNode(x0, y0 + len / 2, len / 2)
) : Node(x0, y0, len) {
    override var x: Double = 0.0
        private set
    override var y: Double = 0.0
        private set
    override var m: Double = 0.0
        private set

    private var particleCount = 0

    private val children = mutableListOf(leftTop, rightTop, rightBottom, leftBottom)

    override fun insert(p: Particle): Node  {
        require(isInside(p))

        val index = children.indexOfFirst { it.isInside(p) }
        children[index] = children[index].insert(p)

        x = (x * m + p.x * p.m) / (m + p.m)
        y = (y * m + p.y * p.m) / (m + p.m)
        m += p.m
        particleCount++

        return this
    }

    override fun step(dt: Double, removedParticles: MutableList<Particle>): Node {
        val mark = removedParticles.size
        children.replaceAll { it.step(dt, removedParticles) }

        // update values
        m = children.sumOf { it.m }
        x = children.sumOf { it.x * it.m } / m
        y = children.sumOf { it.y * it.m } / m
        particleCount -= removedParticles.size - mark

        // try to reinsert removed particles, maybe they belong to a direct neighbor.
        with(removedParticles.listIterator(mark)) {
            while(hasNext()) {
                val p = next()
                if(isInside(p)) {
                    insert(p)
                    remove()
                }
            }
        }

        return tryCollapse()
    }

    private fun tryCollapse(): Node {
        if(particleCount <= 1) {
            val newNode = ExternalNode(x0, y0, len)
            forAllParticles { newNode.insert(it) }
            return newNode
        }

        return this
    }

    override fun updateForce(p: Particle, theta: Double, G: Double, dt: Double) {
        val distance = hypot(p.x - x, p.y - y)
        if (len / distance < theta) {
            p.addForce(x, y, m, G, dt)
        } else {
            for(it in children) {
                it.updateForce(p, theta, G, dt)
            }
        }
    }

    override fun forAllParticles(action: (Particle) -> Unit) {
        for(it in children) {
            it.forAllParticles(action)
        }
    }


}

class ExternalNode(x0: Double, y0: Double, len: Double) : Node(x0, y0, len) {
    override val x: Double get() = particle?.x ?: 0.0
    override val y: Double get() = particle?.y ?: 0.0
    override val m: Double get() = particle?.m ?: 0.0

    private var particle: Particle? = null

    override fun insert(p: Particle): Node {
        return if(particle == null) {
            particle = p
            this
        } else {
            val newNode = InnerNode(x0, y0, len)
            newNode.insert(particle!!)
            newNode.insert(p)
            newNode
        }
    }

    override fun step(dt: Double, removedParticles: MutableList<Particle>): Node {
        if(particle != null) {
            particle!!.step(dt)
            if(!isInside(particle!!)) {
                removedParticles.add(particle!!)
                particle = null
            }
        }

        return this
    }

    override fun updateForce(p: Particle, theta: Double, G: Double, dt: Double) {
        if(particle != null) {
            p.addForce(particle!!.x, particle!!.y, particle!!.m, G, dt)
        }
    }

    override fun forAllParticles(action: (Particle) -> Unit) {
        if(particle != null) action(particle!!)
    }
}