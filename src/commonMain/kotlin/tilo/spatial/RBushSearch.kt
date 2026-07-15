package tilo.spatial

/**
 * Traverses only branches whose cached bounds intersect [query].
 */
internal fun <T> searchIntersections(
    node: RBushNode<T>,
    query: SpatialRect,
    results: MutableList<T>,
) {
    if (!node.hasBounds || !node.bounds.intersects(query)) return

    if (node.leaf) {
        node.entries.forEach { entry ->
            if (entry.bounds.intersects(query)) results.add(entry.item)
        }
        return
    }

    node.children.forEach { child ->
        searchIntersections(child, query, results)
    }
}

internal fun <T> collectItems(
    node: RBushNode<T>,
    results: MutableList<T>,
) {
    if (node.leaf) {
        node.entries.forEach { results.add(it.item) }
    } else {
        node.children.forEach { collectItems(it, results) }
    }
}
