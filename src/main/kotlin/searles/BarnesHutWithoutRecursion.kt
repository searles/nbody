package searles

/**
 * This is a blue print for a hardware-accelerated version of BarnesHut.
 * It does not require recursion and exclusively uses lists with indices
 * that can easily be converted into fixed-size-arrays.
 */

// TODO In order for this to work the max number of nodes must be = number of particles.
// TODO This can be achieved by only allowing inner nodes that have at least 2 branches.
// TODO Also, external nodes without a particle are not allowed.
//class BarnesHutWithoutRecursion(initialBounds: Bounds) {
//    var root: Node
//    val particles = mutableListOf<Particle>()
//
//    init {
//        root = Node(initialBounds)
//        // TODO nodes.add(root)
//    }
//
//    fun add(particle: Particle) {
//        while(!particle.body.position.isInside(root.bounds)) {
//            expand(particle.body.position)
//        }
//
//        root.insert(particle)
//        particles.add(particle)
//    }
//
//    private fun expand(vector: Vector) {
//        // into which direction do I have to expand?
//        val isLeft = vector.x < root.bounds.position.x
//        val isTop = vector.y < root.bounds.position.y
//
//        val newRoot = Node()
//        newRoot.bounds.len = root.bounds.len * 2
//
//        // XXX ugly coupling on these child indices...
//        if(isLeft) {
//            if(isTop) {
//                newRoot.bounds.position.x = root.bounds.position.x - root.bounds.len
//                newRoot.bounds.position.y = root.bounds.position.y - root.bounds.len
//                newRoot.children[3] = root
//            } else {
//                newRoot.bounds.position.x = root.bounds.position.x - root.bounds.len
//                newRoot.bounds.position.y =root.bounds.position.y
//                newRoot.children[2] = root
//            }
//        } else {
//            if(isTop) {
//                newRoot.bounds.position.x = root.bounds.position.x
//                newRoot.bounds.position.y =root.bounds.position.y - root.bounds.len
//                newRoot.children[1] = root
//            } else {
//                newRoot.bounds.position.x = root.bounds.position.x
//                newRoot.bounds.position.y =root.bounds.position.y
//                newRoot.children[0] = root
//            }
//        }
//
//        root.parent = newRoot
//        root = newRoot
//    }
//
//
//
//    fun updateForces() {
//        // TODO reset forces-sum in particles
//        // TODO parallel for-each.
//    }
//
//    fun step(dt: Double) {
//        for(p in particles) {
//            p.body.position.x += p.velocity.x * dt
//            p.body.position.y += p.velocity.y * dt
//
//            if(!p.body.position.isInside(p.node!!.bounds)) {
//                p.node!!.
//            }
//        }
//        forEach(particleIndex in 0 until size) {
//            particle[particleIndex].body.x += particle[particleIndex].body.vx * dt;
//            ...
//            if(isOutOfBounds(particleIndex)) {
//                nodeIndex = particle[particleIndex].nodeIndex
//                unhookParticleFromNode(particle[particleIndex].nodeIndex);
//                free(nodeIndex)
//            }
//        }
//        forEach(particleIndex in 0 until size) {
//            if(isUnhooked(particleIndex)) {
//                add(particleIndex)
//            }
//        }
//        // also update colors.
//    }
//
//
//    class Vector(var x: Double, var y: Double) {
//        fun isInside(bounds: Bounds): Boolean {
//            val x0 = bounds.position.x
//            val y0 = bounds.position.y
//            val len = bounds.len
//
//            return x0 <= x && x <= x0 + len &&
//                    y0 <= y && y <= y0 + len
//        }
//    }
//
//    class Bounds(
//        val position: Vector,
//        var len: Double
//    ) {
//        val mx: Double get() = position.x + len / 2.0
//        val my: Double get() = position.y + len / 2.0
//    }
//
//    class Body(
//        val position: Vector = Vector(0.0, 0.0),
//        var m: Double = 0.0
//    )
//
//    inner class Node(
//        val bounds: Bounds = Bounds(Vector(0.0, 0.0), 0.0),
//        var parent: Node? = null,
//        val children: Array<Node?> = Array(childCount) { null },
//        var particle: Particle? = null,
//        val body: Body = Body()
//    ) {
//        fun isInnerNode(): Boolean {
//            return children.any { it != null }
//        }
//
//        fun isEmptyNode(): Boolean {
//            return this != root && particle == null && !isInnerNode()
//        }
//
//
//        fun insert(particle: Particle) {
//            require(particle.body.position.isInside(bounds))
//
//            if(isInnerNode()) {
//                val childNode = getChildNodeForParticle(particle)
//                childNode.insert(particle) // TODO: Recursion, replace by loop!
//            } else if(this.particle != null) {
//                insertParticleInNonEmptyExternalNode(particle)
//            } else {
//                insertParticleInEmptyExternalNode(particle)
//            }
//        }
//
//        private fun insertParticleInEmptyExternalNode(particle: Particle) {
//            this.particle = particle
//            particle.node = this
//            recalibrate()
//        }
//
//        private fun insertParticleInNonEmptyExternalNode(particle: Particle) {
//            val otherParticle = this.particle!!
//            this.particle = null
//
//            getChildNodeForParticle(particle).let {
//                it.particle = particle
//                particle.node = it
//                it.recalibrate()
//            }
//
//            getChildNodeForParticle(otherParticle).let {
//                it.particle = otherParticle
//                otherParticle.node = it
//                it.recalibrate()
//            }
//
//            recalibrate()
//        }
//
//        private fun getChildNodeForParticle(particle: Particle): Node {
//            // 0 = left top, 1 = right top, 2 = left bottom, 3 = right bottom
//            val isLeft = particle.body.position.x < bounds.mx
//            val isTop = particle.body.position.y < bounds.my
//
//            val childIndex = isLeft.toInt() + 2 * isTop.toInt()
//            var childNode = children[childIndex]
//
//            if (childNode == null) {
//                // allocate new node.
//                childNode = Node()
//                childNode.bounds.position.x = if (isLeft) bounds.position.x else bounds.mx
//                childNode.bounds.position.y = if (isTop) bounds.position.y else bounds.mx
//                childNode.bounds.len = bounds.len / 2
//
//                childNode.parent = this
//                children[childIndex] = childNode
//            }
//
//            return childNode
//        }
//
//        fun unhookParticleFromNode() {
//            assert(particle != null && children.all { it == null })
//            val p = this.particle!!
//
//            p.node = null
//            this.particle = null
//            this.body.m = 0.0;
//
//            // remove it from masses in all parents!
//            var n = parent
//            while(n != null) {
//                n.recalibrate()
//                n = n.parent
//            }
//
//            // TODO and free nodes
//        }
//
//        private fun recalibrate() {
//            if(particle != null) {
//                this.body.m = particle!!.body.m
//                this.body.position.x = particle!!.body.position.x
//                this.body.position.y = particle!!.body.position.y
//
//                return
//            }
//
//            // Recalculate to avoid rounding errors.
//            var m = 0.0
//            var x = 0.0
//            var y = 0.0
//
//            for(childNode in children) {
//                if(childNode != null) {
//                    m += childNode.body.m
//                    x += childNode.body.position.x * childNode.body.m
//                    y += childNode.body.position.y * childNode.body.m
//                }
//            }
//
//            this.body.m = m
//            this.body.position.x = x / m
//            this.body.position.y = y / m
//        }
//    }
//
//    class Particle(
//        val body: Body,
//        val velocity: Vector,
//        var node: Node? = null,
//        var force: Double
//    ) {
//        fun isUnhooked(): Boolean {
//            return node == null
//        }
//    }
//
//    /*fun allocateNode(): Int {
//        for(i in nodes.indices) {
//            if(isFreeNode(i)) {
//                return i;
//            }
//        }
//    }
//
//    fun freeNode(nodeIndex: Int) {
//        var i = nodeIndex
//        while(isEmptyNode(i)) {
//            val parentIndex = nodes[i].parentIndex
//            nodes[i].parentIndex = -1
//
//            for(j in 0 until childCount) {
//                if(nodes[parentIndex].childIndices[j] == i) {
//                    nodes[parentIndex].childIndices[j] = -1
//                }
//            }
//
//            i = parentIndex
//        }
//    }
//
//    fun isFreeNode(i: Int): Boolean {
//        return rootIndex != i && nodes[i].parentIndex == -1
//    }*/
//
//    companion object {
//        // This depends on the dimension. It could be easily done also in 3D.
//        val childCount = 4
//        fun Boolean.toInt() = if (this) 1 else 0
//    }
//}