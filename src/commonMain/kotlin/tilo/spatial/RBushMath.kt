package tilo.spatial

internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int = (value + divisor - 1) / divisor

internal fun ceilSqrt(value: Int): Int {
    var result = 1
    while (result * result < value) result += 1
    return result
}

internal fun intersectionArea(
    a: SpatialRect,
    b: SpatialRect,
): Double {
    val minX = maxOf(a.minX, b.minX)
    val minY = maxOf(a.minY, b.minY)
    val maxX = minOf(a.maxX, b.maxX)
    val maxY = minOf(a.maxY, b.maxY)
    if (minX > maxX || minY > maxY) return 0.0
    return (maxX - minX) * (maxY - minY)
}
