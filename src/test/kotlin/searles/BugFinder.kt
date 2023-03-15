package searles

import org.junit.jupiter.api.Test
import searles.nbody.nbody3d.*

class BugFinder {
    @Test
    fun test() {
        repeat(100000) {
            val bh = BarnesHutTree()
            val points = mutableListOf<Vec>()

            repeat(3) {
                val x = Math.random()
                val y = Math.random()
                val z = Math.random()

                val pt = Vec(x, y, z)
                points.add(pt)

                bh.add(Particle(MassPoint(pt, 1e-3)))
            }

            println(points)

            repeat(10) {
                print("$it;")
                bh.step(1.0, 0.001, 2.0)
            }
        }
    }

    @Test
    fun test2() {
        val bh = BarnesHutTree()

        bh.add(Particle(MassPoint(Vec(0.9228191884386183, 0.7990113141073054, 0.8521724997077941), 1e-3)))
        bh.add(Particle(MassPoint(Vec(0.5133144108433714, 0.7931367601542737, 0.1506038273054915), 1e-3)))
        bh.add(Particle(MassPoint(Vec(0.8496104326612173, 0.8727093462416434, 0.9793483030586081), 1e-3)))

        repeat(8) {
            bh.step(1.0, 0.001, 2.0)
        }

        bh.step(1.0, 0.001, 2.0)
    }
}