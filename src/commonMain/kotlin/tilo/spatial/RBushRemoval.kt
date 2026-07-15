package tilo.spatial

/**
 * Removes the first matching item from [node].
 *
 * Before descending, the item's bounds are used to skip branches that cannot possibly contain it.
 * Empty child nodes are pruned on the way back up.
 */
internal fun <T> removeItem(
    node: RBushNode<T>,
    item: T,
    itemBounds: SpatialRect,
): Boolean {
    if (node.leaf) {
        val index = node.entries.indexOfFirst { it.item == item }
        if (index < 0) return false
        node.entries.removeAt(index)
        node.recalculateBounds()
        return true
    }

    val childIterator = node.children.iterator()
    while (childIterator.hasNext()) {
        val child = childIterator.next()
        if (child.bounds.intersects(itemBounds) && removeItem(child, item, itemBounds)) {
            if (child.childCount == 0) {
                childIterator.remove()
            }
            node.recalculateBounds()
            return true
        }
    }
    return false
}

/**
 * Collapses redundant root levels after removals.
 *
 * R-trees can temporarily end up with a root that has a single child. Removing that level keeps
 * subsequent searches from doing unnecessary work.
 */
internal fun <T> cleanupRoot(root: RBushNode<T>): RBushNode<T> {
    var current = root
    while (!current.leaf && current.children.size == 1) {
        current = current.children.single()
    }
    if (current.childCount == 0) {
        return RBushNode(height = 1, leaf = true)
    }
    return current
}
