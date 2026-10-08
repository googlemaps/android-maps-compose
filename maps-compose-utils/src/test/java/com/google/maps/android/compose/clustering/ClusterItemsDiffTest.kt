/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.clustering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class ClusterItemsDiffTest {
    @Test
    public fun reportsAddedAndRemovedItems() {
        val diff = diffClusterItems(
            previousItems = mapOf("kept" to Item("kept", 1), "removed" to Item("removed", 2)),
            currentItems = mapOf("kept" to Item("kept", 1), "added" to Item("added", 3)),
        )

        assertEquals(listOf(Item("added", 3)), diff.added)
        assertEquals(listOf(Item("removed", 2)), diff.removed)
        assertEquals(emptyList<Pair<Item, Item>>(), diff.updated)
        assertTrue(diff.hasChanges)
    }

    @Test
    public fun reportsUpdatedItemsByStableKey() {
        val old = Item("same-key", 1)
        val current = Item("same-key", 2)
        val diff = diffClusterItems(
            previousItems = mapOf("stable-key" to old),
            currentItems = mapOf("stable-key" to current),
        )

        assertEquals(listOf(old to current), diff.updated)
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.hasChanges)
    }

    @Test
    public fun reportsUnchangedItemsAndNoChanges() {
        val item = Item("same", 1)
        val diff = diffClusterItems(
            previousItems = mapOf("stable-key" to item),
            currentItems = mapOf("stable-key" to item.copy()),
        )

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.updated.isEmpty())
        assertFalse(diff.hasChanges)
    }

    @Test
    public fun noKeyUsesItemEqualityAndHashCodeAsIdentity() {
        val unchanged = Item("same", 1)
        val changed = Item("same", 2)
        val current = clusterItemsByKey(listOf(unchanged.copy(), changed), key = null)

        assertEquals(mapOf(unchanged to unchanged), clusterItemsByKey(listOf(unchanged), key = null))
        assertEquals(setOf(unchanged, changed), current.keys)
        assertEquals(2, current.size)
    }

    @Test
    public fun emptyPreviousCollectionAddsAllItems() {
        val item = Item("first", 1)
        val diff = diffClusterItems(emptyMap(), mapOf("first" to item))

        assertEquals(listOf(item), diff.added)
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.updated.isEmpty())
    }

    private data class Item(val id: String, val value: Int)
}
