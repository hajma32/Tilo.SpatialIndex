package tilo.spatial

/**
 * Mutable Kotlin Multiplatform R-tree inspired by RBush.
 *
 * The index stores only axis-aligned bounding boxes. It does not know anything about map
 * projections, geometry topology, or rendering. Callers provide a small [boundsOf] adapter,
 * and the tree uses those rectangles to quickly reject branches that cannot intersect a query.
 *
 * Typical map usage:
 * 1. Bulk-load a static list of features with [load].
 * 2. On every pan/zoom, call [search] with the visible map bounds.
 * 3. Render only the returned candidates. Exact geometry clipping can still happen later.
 */
class RBush<T>(
    maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val boundsOf: (T) -> SpatialRect
) {
    internal val maxEntries = maxOf(MIN_MAX_ENTRIES, maxEntries)
    internal val minEntries = maxOf(MIN_MIN_ENTRIES, this.maxEntries * MIN_FILL_PERCENT / PERCENT)

    private var root = RBushNode<T>(height = 1, leaf = true)
    private var size = 0

    /**
     * Returns every item currently stored in the tree.
     *
     * The order is tree order, not insertion order.
     */
    fun all(): List<T> = buildList {
        collectItems(root, this)
    }

    /**
     * Removes all items and restores the tree to an empty one-level leaf root.
     */
    fun clear() {
        root = RBushNode(height = 1, leaf = true)
        size = 0
    }

    /**
     * Rebuilds the tree from [items] using STR-style bulk packing.
     *
     * Bulk loading is much faster and produces a shallower, better packed tree than inserting
     * a large static dataset one item at a time.
     */
    fun load(items: Iterable<T>): RBush<T> {
        val entries = items.toList()
        clear()
        if (entries.isEmpty()) return this

        root = RBushBulkLoader(maxEntries, boundsOf).build(entries)
        size = entries.size
        return this
    }

    /**
     * Inserts one item into the existing tree.
     *
     * This is useful for edits and live updates. For replacing a whole dataset, prefer [load].
     */
    fun insert(item: T): RBush<T> {
        val entry = RBushEntry(item, boundsOf(item))
        root = insertEntry(root, entry, maxEntries, minEntries)
        size += 1
        return this
    }

    /**
     * Removes the first item equal to [item].
     *
     * Equality is based on the item's `equals` implementation.
     */
    fun remove(item: T): Boolean {
        val removed = removeItem(root, item, boundsOf(item))
        if (!removed) return false

        size -= 1
        root = cleanupRoot(root)
        root.recalculateBounds()
        return true
    }

    /**
     * Finds all items whose stored bounds intersect [bounds].
     *
     * This returns bbox candidates. If callers need exact geometry intersection, they should
     * run that more expensive check on this much smaller result set.
     */
    fun search(bounds: SpatialRect): List<T> = buildList {
        searchIntersections(root, bounds, this)
    }

    fun isEmpty(): Boolean = size == 0

    fun size(): Int = size

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 9
        const val MIN_MAX_ENTRIES = 4
        const val MIN_MIN_ENTRIES = 2
        const val MIN_FILL_PERCENT = 40
        const val PERCENT = 100
    }
}

fun <T : SpatialItem> RBush(maxEntries: Int = 9): RBush<T> =
    RBush(maxEntries = maxEntries) { it.bounds }
