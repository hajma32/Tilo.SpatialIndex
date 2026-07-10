# Tilo Spatial Index

Kotlin Multiplatform spatial index for fast in-memory bounding-box queries.

Inspired by [RBush](https://github.com/mourner/rbush), a high-performance
JavaScript R-tree spatial index by Volodymyr Agafonkin.

The module provides a small mutable R-tree implementation for indexing points,
rectangles, and geometry-like objects by their axis-aligned bounds. It is aimed
at map and visualization workloads where the same in-memory feature collection
is queried repeatedly as the viewport changes.

## What It Solves

Rendering vector features usually starts with a question like:

> Which features can intersect the current viewport?

A plain list answers that by checking every feature on every pan or zoom. That
is fine for tiny datasets, but it becomes expensive quickly. An R-tree stores
nearby bounding boxes together, so viewport searches can skip whole branches of
features whose cached bounds do not intersect the query.

This index returns bounding-box candidates. If exact geometry intersection is
needed, run that check after the index query on the much smaller result set.

## Current Scope

- Kotlin Multiplatform `commonMain` implementation.
- No JVM-only dependencies.
- Mutable API: `load`, `insert`, `remove`, `search`, `all`, `clear`.
- Optimized for bulk-loading static in-memory feature collections.
- Small public surface based on `SpatialRect` and a caller-provided bounds
  adapter.

## Usage

```kotlin
import tilo.spatial.RBush
import tilo.spatial.SpatialRect

data class Parcel(
    val id: String,
    val bounds: SpatialRect
)

val parcels = listOf(
    Parcel("a", SpatialRect(0.0, 0.0, 10.0, 10.0)),
    Parcel("b", SpatialRect(20.0, 20.0, 30.0, 30.0))
)

val index = RBush<Parcel> { it.bounds }
    .load(parcels)

val visible = index.search(
    SpatialRect(minX = 5.0, minY = 5.0, maxX = 25.0, maxY = 25.0)
)
```

If your item type implements `SpatialItem`, you can use the convenience factory:

```kotlin
import tilo.spatial.RBush
import tilo.spatial.SpatialItem
import tilo.spatial.SpatialRect

data class Feature(
    override val bounds: SpatialRect,
    val label: String
) : SpatialItem

val index = RBush<Feature>().load(features)
```

## Performance Notes

The index is more expensive to build than a plain list, especially when inserting
items one by one. The payoff is repeated spatial querying.

These measurements were taken from the JVM performance test in this repository:

```bash
gradle jvmTest \
  --tests tilo.spatial.RBushPerformanceTest \
  -Dtilo.perf.items=1000000 \
  -Dtilo.perf.queries=2000
```

### 500 Items, 2,000 Viewport Queries

```text
List bulk copy: 0.00 ms
RBush bulk load: 0.83 ms
List incremental add: 0.03 ms
RBush incremental insert: 3.24 ms
List viewport search: 8.53 ms
RBush viewport search: 0.63 ms (13.5x faster than List viewport search)
matchedFeatures=120
```

For one-off searches over a few hundred items, a list can still be perfectly
reasonable. For map-style repeated viewport queries, the index already wins.

### 1,000,000 Items, 2,000 Viewport Queries

```text
List bulk copy: 1.94 ms
RBush bulk load: 208.49 ms
List incremental add: 9.51 ms
RBush incremental insert: 1373.04 ms
List viewport search: 7677.94 ms
RBush viewport search: 9.43 ms (814.3x faster than List viewport search)
matchedFeatures=197750
```

For large in-memory feature collections, the search speedup is the main reason
to use the index. Prefer `load` for static datasets and reserve `insert` for
small incremental updates.

## Credit

This implementation is inspired by
[RBush](https://github.com/mourner/rbush), a high-performance JavaScript R-tree
spatial index by Volodymyr Agafonkin.

RBush is licensed under the MIT License. This module is not intended as a
line-by-line port; it uses a Kotlin Multiplatform implementation tailored to
Tilo's in-memory feature querying needs, while following the same general R-tree
ideas: bounding-box indexing, configurable node size, bulk loading, subtree
selection by bbox enlargement, overlap-aware splitting, and fast viewport
intersection queries.

## License

MIT License. See [LICENSE](LICENSE).
