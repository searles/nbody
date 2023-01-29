package searles

import org.junit.jupiter.api.Test
import java.util.*

class DfsWithoutRecursionTest {
    @Test
    fun `WHEN creating a binary tree THEN all nodes are processed`() {
        val tree =
            Node(
                Node(),
                Node(
                    Node(
                        Node(),
                        Node(
                            Node(),
                            Node(),
                            Node()
                        )
                    ),
                    Node(
                        Node(
                            Node()
                        )
                    )
                ),
                Node()
            )

        dfsWithInt(tree)
    }

    private fun dfsWithInt(root: Node) {
        var node = root
        var position = 0 // no children processed yet

        while(true) {
            // stack contains the child of child node that will be processed next.
            val childIndex = position % (node.child.size + 1)
            position /= (node.child.size + 1)

            if(childIndex == 0) {
                println("Preorder:  ${node.label}, $position")
            }

            if(childIndex < node.child.size) {
                // go down.
                position *= (node.child.size + 1)
                position += childIndex + 1

                node = node.child[childIndex]
                position *= (node.child.size + 1)
            } else {
                println("Postorder: ${node.label}, $position")
                // go up
                if(node.parent == null) return
                node = node.parent!!
            }
        }
    }

    private fun dfsWithStack(root: Node) {
        var node = root
        val stack = Stack<Int>()
        stack.push(0) // no children processed yet

        while(true) {
            // stack contains the child of child node that will be processed next.
            val childIndex = stack.pop()

            if(childIndex == 0) {
                println("Preorder:  ${node.label}, $stack")
            }

            if(childIndex < node.child.size) {
                // go down.
                stack.push(childIndex + 1) // store current index

                node = node.child[childIndex]
                stack.push(0) // and start at child 0.
            } else {
                println("Postorder: ${node.label}, $stack")
                // go up
                if(node.parent == null) return
                node = node.parent!!
            }
        }
    }

    private fun recursiveDfs(tree: Node) {
        println("Preorder:  ${tree.label}")

        for(child in tree.child) {
            recursiveDfs(child)
        }

        println("Postorder: ${tree.label}")
    }

    private fun dfs(tree: Node) {
        var node = tree
        val stack = Stack<Int>()

        stack.push(0)

        while(true) {
            val nextChildIndex = stack.pop()

            if(nextChildIndex in 0 until node.child.size) {
                // go down
                stack.push(nextChildIndex)
                node = node.child[nextChildIndex]
            } else {
                println("Process: ${node.label} at $stack") // Post-Order.
                if(node.parent == null) return // done.
                // go up to next child.
                stack.push(nextChildIndex + 1)
                node = node.parent!!
            }
        }
    }

    class Node(vararg val child: Node) {
        var parent: Node? = null
        var label: String = "1"

        init {
            child.forEach { it.parent = this }
            initLabels()
        }

        private fun initLabels() {
            child.forEachIndexed { index, node ->
                node.label = "$label/${index + 1}"
                node.initLabels()
            }
        }
    }
}