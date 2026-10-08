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

internal data class ClusterItemsDiff<T>(
    val added: List<T>,
    val removed: List<T>,
    val updated: List<Pair<T, T>>,
) {
    val hasChanges: Boolean
        get() = added.isNotEmpty() || removed.isNotEmpty() || updated.isNotEmpty()
}

internal fun <T : Any> clusterItemsByKey(
    items: Collection<T>,
    key: ((T) -> Any)?,
): Map<Any, T> {
    val keyedItems = linkedMapOf<Any, T>()
    items.forEach { item ->
        val itemKey = key?.invoke(item) ?: item
        if (key != null) {
            require(itemKey !in keyedItems) {
                "Clustering item keys must be unique: $itemKey"
            }
        }
        keyedItems[itemKey] = item
    }
    return keyedItems
}

internal fun <T, K> diffClusterItems(
    previousItems: Map<K, T>,
    currentItems: Map<K, T>,
): ClusterItemsDiff<T> {
    val added = mutableListOf<T>()
    val removed = mutableListOf<T>()
    val updated = mutableListOf<Pair<T, T>>()

    previousItems.forEach { (key, previousItem) ->
        val currentItem = currentItems[key]
        if (currentItem == null) {
            removed += previousItem
        } else if (previousItem != currentItem) {
            updated += previousItem to currentItem
        }
    }
    currentItems.forEach { (key, currentItem) ->
        if (key !in previousItems) {
            added += currentItem
        }
    }

    return ClusterItemsDiff(added, removed, updated)
}
