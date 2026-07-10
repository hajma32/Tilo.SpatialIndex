package tilo.spatial

/**
 * Builds a packed tree for a static dataset.
 *
 * The strategy is STR-style packing:
 * 1. Sort items by X.
 * 2. Cut them into vertical slices.
 * 3. Sort each slice by Y.
 * 4. Pack consecutive groups into nodes of up to [maxEntries].
 *
 * This keeps nearby rectangles close together in the tree, which reduces overlap between branches
 * and makes viewport searches visit fewer nodes.
 */
internal class RBushBulkLoader<T>(
    private val maxEntries: Int,
    private val boundsOf: (T) -> SpatialRect
) {
    fun build(items: List<T>): RBushNode<T> {
        var level = packEntries(items.map { item -> RBushEntry(item, boundsOf(item)) })

        while (level.size > 1) {
            level = packLevel(level)
        }

        return level.first()
    }

    private fun packEntries(entries: List<RBushEntry<T>>): List<RBushNode<T>> {
        val sliceSize = sliceSizeFor(entries.size)

        return entries
            .sortedBy { it.bounds.minX }
            .chunked(sliceSize)
            .flatMap { slice ->
                slice
                    .sortedBy { it.bounds.minY }
                    .chunked(maxEntries)
                    .map { leafEntries ->
                        RBushNode(
                            height = 1,
                            leaf = true,
                            entries = leafEntries.toMutableList()
                        ).also { it.recalculateBounds() }
                    }
            }
    }

    private fun packLevel(nodes: List<RBushNode<T>>): List<RBushNode<T>> {
        val sliceSize = sliceSizeFor(nodes.size)

        return nodes
            .sortedBy { it.bounds.minX }
            .chunked(sliceSize)
            .flatMap { slice ->
                slice
                    .sortedBy { it.bounds.minY }
                    .chunked(maxEntries)
                    .map { children ->
                        RBushNode(
                            height = children.first().height + 1,
                            leaf = false,
                            children = children.toMutableList()
                        ).also { it.recalculateBounds() }
                    }
            }
    }

    private fun sliceSizeFor(count: Int): Int {
        val sliceCount = ceilSqrt(ceilDiv(count, maxEntries))
        return sliceCount * maxEntries
    }
}
