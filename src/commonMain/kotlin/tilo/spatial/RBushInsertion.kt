package tilo.spatial

/**
 * Inserts [entry] and returns the current root, or a new root if the old one had to split.
 */
internal fun <T> insertEntry(
    root: RBushNode<T>,
    entry: RBushEntry<T>,
    maxEntries: Int,
    minEntries: Int
): RBushNode<T> {
    val path = mutableListOf<RBushNode<T>>()
    val leaf = chooseSubtree(root, entry.bounds, path)
    leaf.entries.add(entry)
    leaf.recalculateBounds()

    var currentRoot = root

    // Walk back up to the root. Each ancestor's cached bounds may have expanded. If any node
    // overflows, split it and attach the sibling to its parent.
    for (i in path.lastIndex downTo 0) {
        val node = path[i]
        node.recalculateBounds()
        if (node.childCount <= maxEntries) continue

        val sibling = splitNode(node, minEntries)
        if (i == 0) {
            currentRoot = splitRoot(node, sibling)
        } else {
            path[i - 1].children.add(sibling)
        }
    }

    return currentRoot
}

/**
 * Descends to the leaf whose bounding rectangle needs the least enlargement.
 *
 * This is the standard R-tree heuristic: keep the new item in the branch where it changes the
 * spatial coverage as little as possible, then prefer the smaller branch as a tie breaker.
 */
private fun <T> chooseSubtree(
    node: RBushNode<T>,
    bounds: SpatialRect,
    path: MutableList<RBushNode<T>>
): RBushNode<T> {
    path.add(node)
    if (node.leaf) return node

    val next = node.children.minWith(
        compareBy<RBushNode<T>> { it.bounds.enlargementNeeded(bounds) }
            .thenBy { it.bounds.area }
    )
    return chooseSubtree(next, bounds, path)
}

private fun <T> splitRoot(
    left: RBushNode<T>,
    right: RBushNode<T>
): RBushNode<T> =
    RBushNode(
        height = left.height + 1,
        leaf = false,
        children = mutableListOf(left, right)
    ).also { it.recalculateBounds() }

private fun <T> splitNode(
    node: RBushNode<T>,
    minEntries: Int
): RBushNode<T> {
    if (node.leaf) {
        val groups = splitEntries(node.entries, minEntries)
        node.entries.clear()
        node.entries.addAll(groups.first)
        node.recalculateBounds()
        return RBushNode(height = node.height, leaf = true, entries = groups.second.toMutableList())
            .also { it.recalculateBounds() }
    }

    val groups = splitChildren(node.children, minEntries)
    node.children.clear()
    node.children.addAll(groups.first)
    node.recalculateBounds()
    return RBushNode(height = node.height, leaf = false, children = groups.second.toMutableList())
        .also { it.recalculateBounds() }
}

private fun <T> splitEntries(
    entries: List<RBushEntry<T>>,
    minEntries: Int
): Pair<List<RBushEntry<T>>, List<RBushEntry<T>>> {
    val sorted = entries.sortedBy { it.bounds.minX }
    return splitSorted(sorted, minEntries) { group -> SpatialRect.containing(group.map { it.bounds }) }
}

private fun <T> splitChildren(
    children: List<RBushNode<T>>,
    minEntries: Int
): Pair<List<RBushNode<T>>, List<RBushNode<T>>> {
    val sorted = children.sortedBy { it.bounds.minX }
    return splitSorted(sorted, minEntries) { group -> SpatialRect.containing(group.map { it.bounds }) }
}

/**
 * Chooses the best split point for an overflowing node.
 *
 * The simple version here sorts by X and tries every legal split. The winner is the split with the
 * smallest overlap between the two resulting bounding boxes; smaller combined area breaks ties.
 */
private fun <E> splitSorted(
    entries: List<E>,
    minEntries: Int,
    boundsOfGroup: (List<E>) -> SpatialRect
): Pair<List<E>, List<E>> {
    var bestIndex = minEntries
    var bestOverlap = Double.POSITIVE_INFINITY
    var bestArea = Double.POSITIVE_INFINITY
    val lastSplit = entries.size - minEntries

    for (index in minEntries..lastSplit) {
        val left = boundsOfGroup(entries.subList(0, index))
        val right = boundsOfGroup(entries.subList(index, entries.size))
        val overlap = intersectionArea(left, right)
        val area = left.area + right.area
        if (overlap < bestOverlap || overlap == bestOverlap && area < bestArea) {
            bestIndex = index
            bestOverlap = overlap
            bestArea = area
        }
    }

    return entries.subList(0, bestIndex) to entries.subList(bestIndex, entries.size)
}
