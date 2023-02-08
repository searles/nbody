package searles.nbody.nbody3d

data class MassPoint(var pos: Vec = Vec(), var mass: Double = 0.0) {
    fun setToAverage(others: List<MassPoint>) {
        pos.setToZero()
        mass = 0.0

        for(other in others) {
            pos.add(other.pos, other.mass)
            mass += other.mass
        }

        pos.divideBy(mass)
    }

    override fun toString(): String {
        return "{$pos, mass=$mass}"
    }
}