package searles.nbody.nbody3d

sealed class Node(var parent: Branch? = null) {
    abstract val center: Vec
    abstract val gravPt: MassPoint
    abstract fun updateForceForParticle(particle: Particle, theta: Double, G: Double)
    abstract fun recalibrate()
}

class Branch(
    val region: Region,
    val children: Array<Node?> = Array(8) { null }
): Node(null) {
    override val center: Vec
        get() = region.center
    override val gravPt: MassPoint = MassPoint()

    fun contains(vec: Vec): Boolean {
        return region.isInside(vec)
    }

    fun containsInCorrectChild(body: Body): Boolean {
        if(!contains(body.particle.pt.pos)) return false

        val index = children.indexOfFirst { it == body }
        require(index in 0..7)
        return indexOf(body.particle.pt.pos) == index
    }

    fun indexOf(pt: Vec): Int {
        return region.indexOf(pt)
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

        if (2 * region.len / distance < theta) {
            particle.addForce(gravPt, G)
        } else {
            for(it in children) {
                it?.updateForceForParticle(particle, theta, G)
            }
        }
    }
}

class Body(val particle: Particle): Node(null) {
    override val center: Vec
        get() = particle.pt.pos
    override val gravPt: MassPoint
        get() = particle.pt

    override fun updateForceForParticle(particle: Particle, theta: Double, G: Double) {
        particle.addForce(this.particle.pt, G)
    }

    override fun recalibrate() {
        // nothing to do.
    }

    override fun toString(): String {
        return particle.toString()
    }
}