package tilo.spatial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RBushTest {
    @Test
    fun bulkLoadSearchesIntersectingItems() {
        val items = (0 until 100).map { index ->
            TestItem(index, SpatialRect(index.toDouble(), 0.0, index + 0.5, 0.5))
        }
        val tree = RBush<TestItem> { it.bounds }.load(items)

        val result = tree.search(SpatialRect(10.25, -1.0, 12.25, 1.0)).map { it.id }.sorted()

        assertEquals(listOf(10, 11, 12), result)
        assertEquals(100, tree.size())
    }

    @Test
    fun insertAddsItemsToExistingTree() {
        val tree = RBush<TestItem> { it.bounds }
            .load(listOf(TestItem(1, SpatialRect(0.0, 0.0, 1.0, 1.0))))

        tree.insert(TestItem(2, SpatialRect(10.0, 10.0, 11.0, 11.0)))

        assertEquals(listOf(2), tree.search(SpatialRect(9.0, 9.0, 12.0, 12.0)).map { it.id })
        assertEquals(2, tree.size())
    }

    @Test
    fun insertAddsItemsToBulkLoadedTree() {
        val items = (0 until 100).map { index ->
            TestItem(index, SpatialRect(index.toDouble(), 0.0, index + 0.5, 0.5))
        }
        val tree = RBush<TestItem> { it.bounds }.load(items)
        val inserted = TestItem(1_000, SpatialRect(1_000.0, 1_000.0, 1_001.0, 1_001.0))

        tree.insert(inserted)

        assertEquals(listOf(inserted), tree.search(SpatialRect(999.0, 999.0, 1_002.0, 1_002.0)))
        assertEquals(101, tree.size())
    }

    @Test
    fun removeDeletesMatchingItem() {
        val first = TestItem(1, SpatialRect(0.0, 0.0, 1.0, 1.0))
        val second = TestItem(2, SpatialRect(10.0, 10.0, 11.0, 11.0))
        val tree = RBush<TestItem> { it.bounds }.load(listOf(first, second))

        assertTrue(tree.remove(first))
        assertFalse(tree.remove(first))

        assertEquals(listOf(second), tree.all())
        assertEquals(1, tree.size())
    }

    @Test
    fun spatialItemFactoryUsesBoundsProperty() {
        val tree = RBush<TestSpatialItem>().load(
            listOf(TestSpatialItem(SpatialRect(5.0, 5.0, 6.0, 6.0)))
        )

        assertEquals(1, tree.search(SpatialRect(4.0, 4.0, 7.0, 7.0)).size)
    }

    private data class TestItem(
        val id: Int,
        val bounds: SpatialRect
    )

    private data class TestSpatialItem(
        override val bounds: SpatialRect
    ) : SpatialItem
}
