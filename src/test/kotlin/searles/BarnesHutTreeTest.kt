package searles

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import searles.nbody2d.BarnesHutTree
import searles.nbody2d.Particle

class BarnesHutTreeTest {
    @Test
    fun `when adding a particle then particle is stored in tree`() {
        val p = Particle(0.0, 0.0, 1.0, 0.0, 0.0)
        val tree = BarnesHutTree(-1e3, -1e3, 2e3)
        tree.add(p)

        var count = 0

        tree.forAllParticles {
            assertEquals(p, it)
            count ++
        }

        assertEquals(1, count)
    }

    @Test
    fun `when adding three particles then particles are stored in tree`() {
        val sun = Particle(0.0, 0.0, 1.989e30, 0.0, 0.0)
        val earth = Particle(149.6e9, 0.0, 5.972e24, 0.0, 29780.0)
        val moon = Particle(149.6e9 + 384400000, 0.0, 7.348e22, 0.0, 29780.0 + 1022)

        val tree = BarnesHutTree(-1.0, -1.0, 2.0)

        tree.add(earth)
        tree.add(sun)
        tree.add(moon)

        assertEquals(3, tree.particleCount)
    }

    @Test
    fun `when adding two particles then center of mass is in middle`() {
        val p1 = Particle(-1.0, 0.0, 10.0, 0.0, 0.0)
        val p2 = Particle(1.0, 0.0, 10.0, 0.0, 0.0)

        val tree = BarnesHutTree(-1.0, -1.0, 2.0)

        tree.add(p1)
        tree.add(p2)

        assertEquals(0.0, tree.cx)
        assertEquals(0.0, tree.cy)
    }

    @Test
    fun `given tree with two particles when performing one step then both are still in tree`() {
        val p1 = Particle(-1.0, 0.0, 10.0, 1.0, 0.0)
        val p2 = Particle(1.0, 0.0, 10.0, -1.0, 0.0)

        val tree = BarnesHutTree(-1.0, -1.0, 2.0)

        tree.add(p1)
        tree.add(p2)

        tree.step(0.01)
        assertEquals(2, tree.particleCount)
    }
}