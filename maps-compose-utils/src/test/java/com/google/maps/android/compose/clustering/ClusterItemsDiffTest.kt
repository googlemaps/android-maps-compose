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

    @Test
    public fun emptyCollectionsHaveNoChanges() {
        assertTrue(clusterItemsByKey(emptyList<Item>(), key = null).isEmpty())
        assertTrue(clusterItemsByKey(emptyList<Item>(), key = { it.id }).isEmpty())
        val diff = diffClusterItems(emptyMap<String, Item>(), emptyMap())

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.updated.isEmpty())
        assertFalse(diff.hasChanges)
    }

    @Test
    public fun emptyCurrentCollectionRemovesAllItems() {
        val item = Item("first", 1)
        val diff = diffClusterItems(mapOf("first" to item), emptyMap())

        assertEquals(listOf(item), diff.removed)
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.updated.isEmpty())
        assertTrue(diff.hasChanges)
    }

    @Test
    public fun duplicateKeysKeepLastItemAndReportEachDuplicate() {
        val duplicates = mutableListOf<Any>()
        val first = Item("same", 1)
        val last = Item("same", 3)
        val other = Item("other", 4)
        val keyedItems = clusterItemsByKey(
            listOf(first, other, first.copy(value = 2), last),
            key = { it.id },
            onDuplicateKey = { duplicates.add(it) },
        )

        assertEquals(mapOf("same" to last, "other" to other), keyedItems)
        assertEquals(listOf("same", "same"), duplicates)
    }

    @Test
    public fun uniqueExplicitKeysDoNotReportDuplicates() {
        val duplicates = mutableListOf<Any>()
        val item = Item("first", 1)
        val keyedItems = clusterItemsByKey(
            listOf(item),
            key = { it.id },
            onDuplicateKey = { duplicates.add(it) },
        )

        assertEquals(mapOf("first" to item), keyedItems)
        assertTrue(duplicates.isEmpty())
    }

    @Test
    public fun unkeyedEqualItemsAreDeduplicatedWithoutWarning() {
        val duplicates = mutableListOf<Any>()
        val item = Item("same", 1)
        val keyedItems = clusterItemsByKey(
            listOf(item, item.copy()),
            key = null,
            onDuplicateKey = { duplicates.add(it) },
        )

        assertEquals(mapOf(item to item), keyedItems)
        assertTrue(duplicates.isEmpty())
    }

    @Test
    public fun unkeyedChangedItemsAreRemovedAndAddedRatherThanUpdated() {
        val old = Item("same", 1)
        val current = old.copy(value = 2)
        val diff = diffClusterItems(
            clusterItemsByKey(listOf(old), key = null),
            clusterItemsByKey(listOf(current), key = null),
        )

        assertEquals(listOf(old), diff.removed)
        assertEquals(listOf(current), diff.added)
        assertTrue(diff.updated.isEmpty())
    }

    @Test
    public fun reportsAddRemoveAndUpdateInSamePass() {
        val removed = Item("removed", 1)
        val old = Item("updated", 2)
        val current = old.copy(value = 3)
        val added = Item("added", 4)
        val kept = Item("kept", 5)
        val diff = diffClusterItems(
            clusterItemsByKey(listOf(removed, old, kept), key = { it.id }),
            clusterItemsByKey(listOf(current, added, kept.copy()), key = { it.id }),
        )

        assertEquals(listOf(added), diff.added)
        assertEquals(listOf(removed), diff.removed)
        assertEquals(listOf(old to current), diff.updated)
        assertTrue(diff.hasChanges)
    }

    private data class Item(val id: String, val value: Int)
}
