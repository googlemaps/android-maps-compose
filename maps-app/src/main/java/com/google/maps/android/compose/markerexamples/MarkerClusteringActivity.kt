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

package com.google.maps.android.compose.markerexamples

import android.os.Bundle
import android.util.Log
import androidx.compose.ui.text.intl.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm
import com.google.maps.android.clustering.algo.SuperClusterAlgorithm
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.ClusteringMarkerProperties
import com.google.maps.android.compose.clustering.rememberClusterManager
import com.google.maps.android.compose.clustering.rememberClusterRenderer
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.singapore
import com.google.maps.android.compose.singapore2
import kotlin.random.Random

private val TAG = MarkerClusteringActivity::class.simpleName

class MarkerClusteringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoogleMapClustering()
        }
    }
}

@Composable
fun GoogleMapClustering() {
    var itemCount by remember { mutableStateOf(50000) }
    // Generate items stably with remember(itemCount) to prevent state list thrashing
    val items = remember(itemCount) {
        val newItems = ArrayList<MyItem>(itemCount)
        val rnd = Random(42)
        val spread = if (itemCount >= 50000) 4.0 else if (itemCount >= 10000) 2.5 else 1.5
        for (i in 1..itemCount) {
            val position = LatLng(
                singapore2.latitude + (rnd.nextDouble() - 0.5) * spread,
                singapore2.longitude + (rnd.nextDouble() - 0.5) * spread,
            )
            newItems.add(MyItem(position, "Marker $i", "Snippet $i", 0f))
        }
        newItems
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .systemBarsPadding()
    ) {
        GoogleMapClustering(
            items = items,
            itemCount = itemCount,
            onItemCountChange = { itemCount = it },
        )
    }
}

@Composable
fun GoogleMapClustering(
    items: List<MyItem>,
    itemCount: Int,
    onItemCountChange: (Int) -> Unit,
) {
    var clusteringType by remember {
        mutableStateOf(ClusteringType.SuperClusterNative)
    }
    var enableAnimation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(singapore2, 7f)
            }
        ) {
            when (clusteringType) {
                ClusteringType.Default -> {
                    DefaultClustering(
                        items = items,
                    )
                }

                ClusteringType.CustomUi -> {
                    CustomUiClustering(
                        items = items,
                    )
                }

                ClusteringType.CustomRenderer -> {
                    CustomRendererClustering(
                        items = items,
                    )
                }

                ClusteringType.SuperClusterNative -> {
                    SuperClusterNativeClustering(
                        items = items,
                        enableAnimation = enableAnimation,
                    )
                }

                ClusteringType.SuperClusterCompose -> {
                    SuperClusterComposeClustering(
                        items = items,
                        enableAnimation = enableAnimation,
                    )
                }

                ClusteringType.Decorations -> {
                    DecorationsClustering(
                        items = items,
                    )
                }
            }

            MarkerInfoWindow(
                state = rememberUpdatedMarkerState(position = singapore2),
                onClick = {
                    Log.d(TAG, "Non-cluster marker clicked! $it")
                    true
                }
            )
        }

        ClusteringControlBar(
            selectedType = clusteringType,
            onClusteringTypeClick = { clusteringType = it },
            selectedCount = itemCount,
            onCountSelected = onItemCountChange,
            enableAnimation = enableAnimation,
            onAnimationToggle = { enableAnimation = it },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun DefaultClustering(items: List<MyItem>) {
    Clustering(
        items = items,
        // Optional: Handle clicks on clusters, cluster items, and cluster item info windows
        onClusterClick = {
            Log.d(TAG, "Cluster clicked! $it")
            false
        },
        onClusterItemClick = {
            Log.d(TAG, "Cluster item clicked! $it")
            false
        },
        onClusterItemInfoWindowClick = {
            Log.d(TAG, "Cluster item info window clicked! $it")
        },
        // Optional: Custom rendering for non-clustered items
        clusterItemContent = null
    )
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun CustomUiClustering(items: List<MyItem>) {
    var selectedItem by remember { mutableStateOf<MyItem?>(null) }
    Clustering(
        items = items,
        // Optional: Handle clicks on clusters, cluster items, and cluster item info windows
        onClusterClick = {
            Log.d(TAG, "Cluster clicked! $it")
            false
        },
        onClusterItemClick = {
            Log.d(TAG, "Cluster item clicked! $it")
            selectedItem = if (selectedItem == it) null else it
            false
        },
        onClusterItemInfoWindowClick = {
            Log.d(TAG, "Cluster item info window clicked! $it")
        },
        // Optional: Custom rendering for clusters
        clusterContent = { cluster ->
            CircleContent(
                modifier = Modifier.size(40.dp),
                text = "%,d".format(Locale.current.platformLocale, cluster.size),
                color = Color.Blue,
            )
        },
        // Optional: Custom rendering for non-clustered items
        clusterItemContent = { item ->
            val isSelected = item == selectedItem
            if (isSelected) {
                ClusteringMarkerProperties(
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 1.0f
                )
            }
            CircleContent(
                modifier = Modifier.size(if (isSelected) 40.dp else 20.dp),
                text = "",
                color = if (isSelected) Color.Red else Color.Green,
            )
        },
        clusterContentAnchor = Offset(0.5f, 0.5f),
        // Optional: Customization hook for clusterManager and renderer when they're ready
        onClusterManager = { clusterManager ->
            (clusterManager.renderer as DefaultClusterRenderer).minClusterSize = 2
        },
    )
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun CustomRendererClustering(items: List<MyItem>) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val clusterManager = rememberClusterManager<MyItem>()

    // Here the clusterManager is being customized with a NonHierarchicalViewBasedAlgorithm.
    // This speeds up by a factor the rendering of items on the screen.
    clusterManager?.setAlgorithm(
        NonHierarchicalViewBasedAlgorithm(
            screenWidth.value.toInt(),
            screenHeight.value.toInt()
        )
    )
    val renderer = rememberClusterRenderer(
        clusterContent = { cluster ->
            CircleContent(
                modifier = Modifier.size(40.dp),
                text = "%,d".format(Locale.current.platformLocale, cluster.size),
                color = Color.Green,
            )
        },
        clusterItemContent = {
            CircleContent(
                modifier = Modifier.size(20.dp),
                text = "",
                color = Color.Green,
            )
        },
        clusterManager = clusterManager,
    )

    SideEffect {
        clusterManager ?: return@SideEffect
        clusterManager.setOnClusterClickListener {
            Log.d(TAG, "Cluster clicked! $it")
            false
        }
        clusterManager.setOnClusterItemClickListener {
            Log.d(TAG, "Cluster item clicked! $it")
            false
        }
        clusterManager.setOnClusterItemInfoWindowClickListener {
            Log.d(TAG, "Cluster item info window clicked! $it")
        }
    }
    SideEffect {
        if (clusterManager?.renderer != renderer) {
            clusterManager?.renderer = renderer ?: return@SideEffect
        }
    }

    if (clusterManager != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }

}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun DecorationsClustering(items: List<MyItem>) {
    Clustering(
        items = items,
        clusterItemDecoration = { item ->
            Circle(
                center = item.position,
                radius = 10000.0,
                fillColor = Color.Blue.copy(alpha = 0.2f),
                strokeColor = Color.Blue,
                strokeWidth = 2f
            )
        }
    )
}

@Composable
private fun CircleContent(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        shape = CircleShape,
        color = color,
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun SuperClusterNativeClustering(items: List<MyItem>, enableAnimation: Boolean) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val clusterManager = rememberClusterManager<MyItem>()
    var renderer by remember { mutableStateOf<DefaultClusterRenderer<MyItem>?>(null) }

    LaunchedEffect(clusterManager, screenWidth, screenHeight) {
        clusterManager?.setAlgorithm(
            SuperClusterAlgorithm(
                radius = 120.0,
                extent = 512.0,
                viewWidth = screenWidth.value.toInt(),
                viewHeight = screenHeight.value.toInt(),
            )
        )
    }

    MapEffect(clusterManager, enableAnimation) { map ->
        clusterManager ?: return@MapEffect
        val r = DefaultClusterRenderer(context, map, clusterManager).apply {
            setAnimation(enableAnimation)
            useCompactNumberFormatting = true
            maxNonZeroDigits = 1
            minClusterSize = 2
        }
        clusterManager.renderer = r
        renderer = r
    }

    if (clusterManager != null && renderer != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun SuperClusterComposeClustering(items: List<MyItem>, enableAnimation: Boolean) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val clusterManager = rememberClusterManager<MyItem>()

    LaunchedEffect(clusterManager, screenWidth, screenHeight) {
        clusterManager?.setAlgorithm(
            SuperClusterAlgorithm(
                radius = 120.0,
                extent = 512.0,
                viewWidth = screenWidth.value.toInt(),
                viewHeight = screenHeight.value.toInt(),
            )
        )
    }

    // Pass clusterItemContent = null so unclustered pins use lightweight native markers instead of creating thousands of ComposeViews!
    val renderer = rememberClusterRenderer(
        clusterContent = { cluster ->
            CircleContent(
                modifier = Modifier.size(46.dp),
                text = "%,d".format(Locale.current.platformLocale, cluster.size),
                color = Color(0xFF6200EE),
            )
        },
        clusterItemContent = null,
        clusterManager = clusterManager,
    )

    SideEffect {
        clusterManager ?: return@SideEffect
        (clusterManager.renderer as? DefaultClusterRenderer)?.setAnimation(enableAnimation)
        clusterManager.setOnClusterClickListener {
            Log.d(TAG, "SuperCluster clicked! size=${it.size} items=${it.items.size}")
            false
        }
    }
    SideEffect {
        if (clusterManager?.renderer != renderer) {
            clusterManager?.renderer = renderer ?: return@SideEffect
        }
    }

    if (clusterManager != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

@Composable
private fun ClusteringControlBar(
    selectedType: ClusteringType,
    onClusteringTypeClick: (ClusteringType) -> Unit,
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    enableAnimation: Boolean,
    onAnimationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var algoMenuExpanded by remember { mutableStateOf(false) }
    var countMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .padding(top = 10.dp, start = 8.dp, end = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Algorithm Dropdown Popup Menu
            Box {
                FilledTonalButton(
                    onClick = { algoMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = when (selectedType) {
                            ClusteringType.SuperClusterNative -> "SuperCluster ⚡"
                            ClusteringType.SuperClusterCompose -> "Compose Badges"
                            ClusteringType.CustomRenderer -> "View-Based"
                            ClusteringType.CustomUi -> "Custom UI"
                            ClusteringType.Default -> "Default"
                            ClusteringType.Decorations -> "Decorations"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Algorithm",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = algoMenuExpanded,
                    onDismissRequest = { algoMenuExpanded = false }
                ) {
                    ClusteringType.entries.forEach { type ->
                        val isSelected = type == selectedType
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (type) {
                                        ClusteringType.SuperClusterNative -> "SuperCluster ⚡ (Native Fast)"
                                        ClusteringType.SuperClusterCompose -> "SuperCluster ⚡ (Compose Badges)"
                                        ClusteringType.CustomRenderer -> "Custom Renderer (View-Based)"
                                        ClusteringType.CustomUi -> "Custom UI"
                                        ClusteringType.Default -> "Default (Distance-Based)"
                                        ClusteringType.Decorations -> "Decorations"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            trailingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                onClusteringTypeClick(type)
                                algoMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Dataset Size Dropdown Popup Menu (including 50K and 100K)
            Box {
                FilledTonalButton(
                    onClick = { countMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = when {
                            selectedCount >= 1000 -> "${selectedCount / 1000}K items"
                            else -> "$selectedCount items"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Dataset Size",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = countMenuExpanded,
                    onDismissRequest = { countMenuExpanded = false }
                ) {
                    listOf(10, 500, 2500, 10000, 25000, 50000, 100000).forEach { count ->
                        val isSelected = count == selectedCount
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when {
                                        count >= 1000 -> "%,d items (${count / 1000}K)".format(count)
                                        else -> "$count items"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            trailingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                onCountSelected(count)
                                countMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Immediately Visible Animations Toggle
            FilterChip(
                selected = enableAnimation,
                onClick = { onAnimationToggle(!enableAnimation) },
                label = {
                    Text(
                        text = if (enableAnimation) "Anim: ON" else "Anim: OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

private enum class ClusteringType {
    Default,
    CustomUi,
    CustomRenderer,
    SuperClusterNative,
    SuperClusterCompose,
    Decorations,
}

data class MyItem(
    override val position: LatLng,
    override val title: String,
    override val snippet: String,
    override val zIndex: Float,
) : ClusterItem
