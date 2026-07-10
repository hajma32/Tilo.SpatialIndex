package tilo.spatial

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class RBushPerformanceTest {
    @Test
    fun compareBulkLoadBulkInsertAndSearchAgainstPlainList() {
        val itemCount = System.getProperty("tilo.perf.items")?.toInt() ?: 100_000
        val queryCount = System.getProperty("tilo.perf.queries")?.toInt() ?: 2_000
        val items = generateItems(itemCount = itemCount)
        val queries = generateQueries(queryCount = queryCount)

        val listLoad = measure("List bulk copy") {
            items.toMutableList()
        }

        val treeLoad = measure("RBush bulk load") {
            RBush<TestFeature> { it.bounds }.load(items)
        }

        val listInsert = measure("List incremental add") {
            mutableListOf<TestFeature>().apply {
                items.forEach(::add)
            }
        }

        val treeInsert = measure("RBush incremental insert") {
            RBush<TestFeature> { it.bounds }.apply {
                items.forEach(::insert)
            }
        }

        val tree = treeLoad.value
        val plainList = listLoad.value

        val listSearch = measure("List viewport search") {
            queries.sumOf { query ->
                plainList.count { item -> item.bounds.intersects(query) }
            }
        }

        val treeSearch = measure("RBush viewport search") {
            queries.sumOf { query ->
                tree.search(query).size
            }
        }

        assertEquals(listSearch.value, treeSearch.value)

        println(
            buildString {
                appendLine()
                appendLine("RBush performance comparison")
                appendLine("--------------------------------")
                appendLine("items=${items.size}, queries=${queries.size}")
                appendLine(listLoad.summary())
                appendLine(treeLoad.summary(relativeTo = listLoad))
                appendLine(listInsert.summary())
                appendLine(treeInsert.summary(relativeTo = listInsert))
                appendLine(listSearch.summary())
                appendLine(treeSearch.summary(relativeTo = listSearch))
                appendLine("matchedFeatures=${treeSearch.value}")
            }
        )
    }

    private fun generateItems(itemCount: Int): List<TestFeature> {
        val columns = 400
        return List(itemCount) { index ->
            val x = (index % columns).toDouble() * 10.0
            val y = (index / columns).toDouble() * 10.0
            TestFeature(
                id = index,
                bounds = SpatialRect(
                    minX = x,
                    minY = y,
                    maxX = x + 4.0,
                    maxY = y + 4.0
                )
            )
        }
    }

    private fun generateQueries(queryCount: Int): List<SpatialRect> {
        val columns = 400
        val rows = 250
        return List(queryCount) { index ->
            val x = ((index * 37) % columns).toDouble() * 10.0
            val y = ((index * 53) % rows).toDouble() * 10.0
            SpatialRect(
                minX = x,
                minY = y,
                maxX = x + 90.0,
                maxY = y + 90.0
            )
        }
    }

    private inline fun <T> measure(
        name: String,
        block: () -> T
    ): Measurement<T> {
        // One warmup keeps class loading and first JIT passes out of the reported number.
        block()
        val start = System.nanoTime()
        val value = block()
        val elapsedNanos = System.nanoTime() - start
        return Measurement(name, elapsedNanos, value)
    }

    private data class TestFeature(
        val id: Int,
        val bounds: SpatialRect
    )

    private data class Measurement<T>(
        val name: String,
        val elapsedNanos: Long,
        val value: T
    ) {
        fun summary(
            relativeTo: Measurement<*>? = null
        ): String {
            val millis = elapsedNanos / NANOS_PER_MILLI
            val base = "$name: ${millis.formatMillis()} ms"
            if (relativeTo == null) return base

            val ratio = relativeTo.elapsedNanos.toDouble() / elapsedNanos.toDouble()
            val suffix = if (ratio >= 1.0) {
                "${ratio.formatRatio()}x faster than ${relativeTo.name}"
            } else {
                "${(1.0 / ratio).formatRatio()}x slower than ${relativeTo.name}"
            }
            return "$base ($suffix)"
        }
    }

    private companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0

        private fun Double.formatMillis(): String =
            (this * 100.0).roundToInt().let { rounded ->
                "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}"
            }

        private fun Double.formatRatio(): String =
            (this * 10.0).roundToInt().let { rounded ->
                "${rounded / 10}.${rounded % 10}"
            }
    }
}
