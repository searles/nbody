package searles.nbody.nbody3d

sealed class Node(var parent: Branch? = null, val center: Vec, val gravPt: MassPoint) {
    abstract fun updateForceForParticle(particle: Particle, theta: Double, G: Double)
    abstract fun recalibrate()
}

class Branch(
    center: Vec, var len: Double,
    val children: Array<Node?> = Array(8) { null }
): Node(null, center, MassPoint()) {
    fun contains(vec: Vec): Boolean {
        return BarnesHutTree.isInside(vec, center, this.len)
    }

    fun containsInCorrectChild(body: Body): Boolean {
        if(!contains(body.particle.pt.pos)) return false

        val index = children.indexOfFirst { it == body }
        require(index in 0..7)
        return indexOf(body.particle.pt.pos) == index
    }

    fun indexOf(pt: Vec): Int {
        require(contains(pt))

        return when {
            pt.x <= center.x && pt.y <= center.y && pt.z <= center.z -> 0
            pt.x > center.x && pt.y <= center.y && pt.z <= center.z -> 1
            pt.x > center.x && pt.y > center.y && pt.z <= center.z -> 2
            pt.x <= center.x && pt.y > center.y && pt.z <= center.z -> 3
            pt.x <= center.x && pt.y <= center.y && pt.z > center.z -> 4
            pt.x > center.x && pt.y <= center.y && pt.z > center.z -> 5
            pt.x > center.x && pt.y > center.y && pt.z > center.z -> 6
            else -> 7
        }
    }

    fun shrinkForDistinctChildren(pt0: Vec, pt1: Vec) {
        while(true) {
            val index0 = indexOf(pt0)
            val index1 = indexOf(pt1)

            if(index0 != index1) {
                break
            }

            // There is a very small chance of rounding errors in the last double-digit.
            len /= 2

            when(index0) {
                0 -> { center.x -= len; center.y -= len; center.z -= len }
                1 -> { center.x += len; center.y -= len; center.z -= len }
                2 -> { center.x += len; center.y += len; center.z -= len }
                3 -> { center.x -= len; center.y += len; center.z -= len }
                4 -> { center.x -= len; center.y -= len; center.z += len }
                5 -> { center.x += len; center.y -= len; center.z += len }
                6 -> { center.x += len; center.y += len; center.z += len }
                else -> { center.x -= len; center.y += len; center.z += len }
            }
        }
    }

    override fun recalibrate() {
        children.filterNotNull().forEach {
            it.recalibrate()
        }

        gravPt.setToAverage(children.mapNotNull { it?.gravPt })
    }

    override fun updateForceForParticle(particle: Particle, theta: Double, G: Double) {
        // This uses recursion. We will need a path of length
        // log2(2 * bodies.size - 1) to store the current path.
        // Use 32, because 2^32 > 4 Bio.
        val distance = particle.pt.pos.distance(gravPt.pos)

        if (2 * len / distance < theta) {
            particle.addForce(gravPt, G)
        } else {
            for(it in children) {
                it?.updateForceForParticle(particle, theta, G)
            }
        }
    }
}

class Body(val particle: Particle): Node(null, particle.pt.pos, particle.pt) {
    override fun updateForceForParticle(particle: Particle, theta: Double, G: Double) {
        particle.addForce(this.particle.pt, G)
    }

    override fun recalibrate() {
        // nothing to do.
    }
}