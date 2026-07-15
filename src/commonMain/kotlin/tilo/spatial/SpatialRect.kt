package tilo.spatial

/**
 * Axis-aligned rectangle used by spatial indexes.
 */
data class SpatialRect(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
) {
    init {
        require(minX <= maxX) { "minX must be <= maxX" }
        require(minY <= maxY) { "minY must be <= maxY" }
    }

    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val area: Double get() = width * height
    val margin: Double get() = width + height

    fun intersects(other: SpatialRect): Boolean =
        maxX >= other.minX &&
            other.maxX >= minX &&
            maxY >= other.minY &&
            other.maxY >= minY

    fun contains(other: SpatialRect): Boolean =
        minX <= other.minX &&
            minY <= other.minY &&
            maxX >= other.maxX &&
            maxY >= other.maxY

    fun union(other: SpatialRect): SpatialRect =
        SpatialRect(
            minX = minOf(minX, other.minX),
            minY = minOf(minY, other.minY),
            maxX = maxOf(maxX, other.maxX),
            maxY = maxOf(maxY, other.maxY),
        )

    fun enlargementNeeded(other: SpatialRect): Double = union(other).area - area

    companion object {
        fun containing(rects: Iterable<SpatialRect>): SpatialRect {
            val iterator = rects.iterator()
            require(iterator.hasNext()) { "Cannot create SpatialRect from an empty collection" }

            var bounds = iterator.next()
            while (iterator.hasNext()) {
                bounds = bounds.union(iterator.next())
            }
            return bounds
        }
    }
}
