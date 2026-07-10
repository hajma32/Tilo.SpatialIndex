package tilo.spatial

internal data class RBushEntry<T>(
    val item: T,
    val bounds: SpatialRect
)

/**
 * Internal tree node.
 *
 * Leaf nodes contain item entries. Branch nodes contain child nodes. Every node caches a bounding
 * rectangle that covers all of its children; search uses that cached rectangle to skip whole
 * branches at once.
 */
internal class RBushNode<T>(
    val height: Int,
    val leaf: Boolean,
    val children: MutableList<RBushNode<T>> = mutableListOf(),
    val entries: MutableList<RBushEntry<T>> = mutableListOf()
) {
    var bounds: SpatialRect = EMPTY_BOUNDS
        private set
    var hasBounds: Boolean = false
        private set

    val childCount: Int get() = if (leaf) entries.size else children.size

    fun recalculateBounds() {
        if (childCount == 0) {
            bounds = EMPTY_BOUNDS
            hasBounds = false
            return
        }

        bounds = if (leaf) {
            SpatialRect.containing(entries.map { it.bounds })
        } else {
            SpatialRect.containing(children.map { it.bounds })
        }
        hasBounds = true
    }

    private companion object {
        val EMPTY_BOUNDS = SpatialRect(0.0, 0.0, 0.0, 0.0)
    }
}
