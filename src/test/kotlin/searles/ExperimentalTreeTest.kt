package searles

import org.junit.jupiter.api.Test
import searles.nbody2d.BalancedBarnesHutTree
import searles.nbody2d.Body

class ExperimentalTreeTest {
    @Test
    fun smokeTest() {
        repeat(1000) {
            val bodies = mutableListOf<Body>()
            val tree = BalancedBarnesHutTree()

            repeat(1000) {
                val body = Body(Math.random(), Math.random(), 1.0, 0.0, 0.0) // make bounds more transparent.
                tree.add(body)
                bodies.add(body)
            }

            repeat(1000) {
                bodies.forEach {
                    tree.updateForce(it, 1.0, 0.1, 0.1)
                }

                bodies.forEach {
                    tree.step(it, 0.1)
                }
            }
        }
    }

    @Test
    fun testBadBounds() {
        val bodies = listOf(
            Body(x = 0.7818315926742222, y = 0.6480239013617937, mass = 1.0, vx = 0.0, vy = 0.0),
            Body(x = 0.8013378610995523, y = 0.677660999544019, mass = 1.0, vx = 0.0, vy = 0.0),
            Body(x = 0.7519636124809339, y = 0.40893230651909807, mass = 1.0, vx = 0.0, vy = 0.0)
        )

        val tree = BalancedBarnesHutTree()

        bodies.forEach { tree.add(it) ; tree.validate() }
        bodies.forEach { tree.updateForce(it, 1.0, 0.1, 0.1) ; tree.validate() }
        bodies.forEach { tree.step(it, 0.1) ; tree.validate() }
        bodies.forEach { tree.updateForce(it, 1.0, 0.1, 0.1) ; tree.validate() }
        bodies.forEach { tree.step(it, 0.1) ; tree.validate() }
    }
}