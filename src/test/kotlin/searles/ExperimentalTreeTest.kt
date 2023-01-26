package searles

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ExperimentalTreeTest {
    @Test
    fun `WHEN adding a body THEN tree contains body`() {
        val tree = BalancedBarnesHutTree()
        tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0))

        assert(tree.root is BalancedBarnesHutTree.Body)
    }

    @Test
    fun `WHEN adding two nodes THEN root is branching`() {
        val tree = BalancedBarnesHutTree()
        tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0))
        tree.add(BalancedBarnesHutTree.Body(0.5, 0.5, 1.0, 0.0, 0.0))

        assertTrue(tree.root is BalancedBarnesHutTree.Branch)
        assertTrue((tree.root as BalancedBarnesHutTree.Branch).child[0] is BalancedBarnesHutTree.Body)
        assertNull((tree.root as BalancedBarnesHutTree.Branch).child[1])
        assert((tree.root as BalancedBarnesHutTree.Branch).child[2] is BalancedBarnesHutTree.Body)
        assertNull((tree.root as BalancedBarnesHutTree.Branch).child[3])
    }

    @Test
    fun `WHEN two nodes are added close to each other THEN their parent shrinks`() {
        val tree = BalancedBarnesHutTree()
        tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0))
        tree.add(BalancedBarnesHutTree.Body(2.0, 2.0, 1.0, 0.0, 0.0)) // added to children[2]
        tree.add(BalancedBarnesHutTree.Body(1.9, 1.9, 1.0, 0.0, 0.0)) // children[2] should now shrink

        assertTrue(tree.root is BalancedBarnesHutTree.Branch)
        val root = (tree.root as BalancedBarnesHutTree.Branch)
        assertNotNull(root.child[0] is BalancedBarnesHutTree.Body)
        assertNull(root.child[1])
        assertEquals(0.062500000000625, (root.child[2] as BalancedBarnesHutTree.Branch).len)
    }

    @Test
    fun `WHEN body is added outside of root bounds THEN root is expanded`() {
        val tree = BalancedBarnesHutTree()
        tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0))
        tree.add(BalancedBarnesHutTree.Body(2.0, 2.0, 1.0, 0.0, 0.0))
        tree.add(BalancedBarnesHutTree.Body(-7.0, -7.0, 1.0, 0.0, 0.0))

        val root = (tree.root as BalancedBarnesHutTree.Branch)

        assertEquals(8.00000000008, root.len)
        assertEquals(-5.00000000006, root.x)
        assertEquals(-5.00000000006, root.y)
    }

    @Test
    fun `WHEN body is added above a shrunk node THEN new branch is inserted`() {
        val tree = BalancedBarnesHutTree()
        tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0))
        tree.add(BalancedBarnesHutTree.Body(2.0, 2.0, 1.0, 0.0, 0.0)) // added to children[2]
        tree.add(BalancedBarnesHutTree.Body(1.9, 1.9, 1.0, 0.0, 0.0)) // children[2] should now shrink
        tree.add(BalancedBarnesHutTree.Body(1.2, 1.2, 1.0, 0.0, 0.0))

        val root = (tree.root as BalancedBarnesHutTree.Branch)

        assertEquals(1.0, root.x)
        assertEquals(1.0, root.y)
    }

    @Test
    fun smokeTest() {
        repeat(10000) {
            val tree = BalancedBarnesHutTree()
            tree.add(BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)) // make bounds more transparent.

            repeat(3) {
                tree.check()
                tree.add(BalancedBarnesHutTree.Body(Math.random(), Math.random(), 1.0, 0.0, 0.0))
            }

            tree.check()
        }
    }

    @Test
    fun `GIVEN root is a body WHEN body is unlinked THEN tree is empty`() {
        val tree = BalancedBarnesHutTree()

        val body = BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)

        tree.add(body)
        tree.unlink(body)

        assertNull(body.parent)
        assertNull(tree.root)
    }

    @Test
    fun `GIVEN tree with 2 bodies WHEN one body is unlinked THEN root is body`() {
        val tree = BalancedBarnesHutTree()

        val body0 = BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)
        val body1 = BalancedBarnesHutTree.Body(1.0, 1.0, 1.0, 0.0, 0.0)

        tree.add(body0)
        tree.add(body1)

        tree.unlink(body0)

        assertEquals(tree.root, body1)
    }

    @Test
    fun `GIVEN tree with two bodies THEN mass is sum of bodies`() {
        val tree = BalancedBarnesHutTree()

        val body0 = BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)
        val body1 = BalancedBarnesHutTree.Body(1.0, 1.0, 1.0, 0.0, 0.0)

        tree.add(body0)
        tree.add(body1)

        assertEquals(2.0, tree.root!!.mass)
    }

    @Test
    fun `GIVEN tree WHEN adding body THEN mass is sum of bodies`() {
        val tree = BalancedBarnesHutTree()

        val body0 = BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)
        val body1 = BalancedBarnesHutTree.Body(1.0, 1.0, 1.0, 0.0, 0.0)
        val body2 = BalancedBarnesHutTree.Body(0.9, 0.9, 1.0, 0.0, 0.0)

        tree.add(body0)
        tree.add(body1)
        tree.add(body2)

        val body = BalancedBarnesHutTree.Body(0.95, 0.95, 1.0, 0.0, 0.0)

        tree.add(body)

        assertEquals(4.0, tree.root!!.mass)
    }

    @Test
    fun `WHEN unlinking a body THEN center of gravity should move`() {
        val tree = BalancedBarnesHutTree()

        repeat(100) {
            tree.add(BalancedBarnesHutTree.Body(Math.random(), Math.random(), 1.0, 0.0, 0.0))
            assertEquals(it.toDouble() + 1.0, tree.root!!.mass)
        }

        assertEquals(100.0, tree.root?.mass)
        assertTrue(abs(tree.root!!.gx - 0.5) < 0.01)
        assertTrue(abs(tree.root!!.gy - 0.5) < 0.01)
    }

    @Test
    fun `WHEN unlinking a body AND adding it again THEN center of gravity should remain the same`() {
        val tree = BalancedBarnesHutTree()

        val body0 = BalancedBarnesHutTree.Body(0.0, 0.0, 1.0, 0.0, 0.0)
        val body1 = BalancedBarnesHutTree.Body(1.0, 1.0, 1.0, 0.0, 0.0)

        tree.add(body0)
        tree.add(body1)

        tree.unlink(body0)

        assertEquals(tree.root, body1)
    }
}