package tilo.spatial

/**
 * Item that can expose its bounds to a spatial index.
 */
interface SpatialItem {
    val bounds: SpatialRect
}
