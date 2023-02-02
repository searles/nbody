package searles

import org.junit.jupiter.api.Test

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

        dfsWithoutRecursion(tree)
    }

    @Test
    fun `WHEN creating a list THEN every node is traversed`() {
        val tree = createFullBinaryTree(10)

        dfsWithoutRecursion2(tree)
    }

    private fun createList(length: Int): Node {
        var node = Node()

        repeat(length - 1) {
            node = Node(node)
        }

        return node
    }

    private fun createFullBinaryTree(depth: Int): Node {
        if(depth <= 1) return Node()

        return Node(createFullBinaryTree(depth - 1), createFullBinaryTree(depth - 1))
    }

    /*
    def dfs(root):
    node = root
    while True:
        visit(node)
        if node.first_child:
            node = node.first_child      # walk down
        else:
            while not node.next_sibling:
                if node is root:
                    return
                node = node.parent       # walk up ...
            node = node.next_sibling     # ... and right

     */

    private fun dfsWithoutRecursion2(root: Node) {
        var node = root

        while(true) {
            println("Preorder  ${node.label}.")

            val firstChild = node.child.firstOrNull()

            if(firstChild != null) {
                node = firstChild
            } else {
                while(true) {
                    println("Postorder ${node.label}")

                    val nextSibling = node.parent?.child?.getOrNull(node.parent!!.child.indexOf(node) + 1)

                    if(nextSibling == null) {
                        node = node.parent ?: return
                    } else {
                        node = nextSibling
                        break
                    }
                }
            }
        }
    }

    private fun dfsWithoutRecursion(root: Node) {
        var lastNode: Node? = null
        var node = root

        while(true) {
            var childIndex: Int = -1

            if(lastNode == node.parent) {
                println("Preorder  ${node.label}.")
            } else {
                childIndex = node.child.indexOf(lastNode)
            }

            val nextSiblingIndex = childIndex + 1

            if(nextSiblingIndex < node.child.size) {
                lastNode = node
                node = node.child[nextSiblingIndex]
            } else {
                // No more siblings
                println("Postoder ${node.label}.")

                lastNode = node
                node = node.parent ?: return
            }
        }
    }

    private fun dfsWithRecursion(tree: Node) {
        println("Preorder:  ${tree.label}")

        for(child in tree.child) {
            dfsWithRecursion(child)
        }

        println("Postorder: ${tree.label}")
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