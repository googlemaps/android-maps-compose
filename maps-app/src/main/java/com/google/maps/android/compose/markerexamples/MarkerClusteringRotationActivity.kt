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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.ClusteringMarkerProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.singapore2
import kotlin.random.Random

/**
 * A dedicated sample activity demonstrating dynamic marker rotation across clustered and
 * non-clustered items using the [Clustering] API in `android-maps-compose`.
 *
 * ### Key Concepts Demonstrated
 * 1. **Two Approaches for Applying Marker Properties (`rotation`, `anchor`, `zIndex`):**
 *    * **Top-Level `Clustering(...)` Parameters:** Passing `clusterContentRotation` and
 *      `clusterItemContentRotation` directly to the [Clustering] composable. This is the recommended
 *      approach when all clusters or items share a uniform dynamic property state.
 *    * **`ClusteringMarkerProperties(...)` inside Custom Composables:** Calling [ClusteringMarkerProperties]
 *      directly inside your `clusterContent` or `clusterItemContent` composables. This is the ideal
 *      pattern when individual items require data-driven properties (for example, rotating a directional
 *      navigation arrow or vehicle icon according to each specific item's heading or state).
 *
 * 2. **Best Practice: Aligning the Anchor Point (`Offset(0.5f, 0.5f)`):**
 *    * By default across the [Clustering] API, marker anchors (`clusterContentAnchor` and
 *      `clusterItemContentAnchor`) default to `Offset(0.5f, 1.0f)`, which corresponds to **bottom-center**
 *      (the bottom tip of a traditional pin-drop marker).
 *    * When rotating symmetrical icons (like circles, directional triangles, or avatars) around a
 *      bottom-center anchor, the icons will orbit around their bottom edge like a pendulum.
 *    * **Rule of Thumb:** To spin symmetrical icons smoothly in place around their exact center point,
 *      always explicitly set the anchor to dead center (`Offset(0.5f, 0.5f)`), as demonstrated in this sample.
 */
class MarkerClusteringRotationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoogleMapClusteringRotation()
        }
    }
}

/**
 * Main composable containing the interactive map, clustered markers, and rotation control panel.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GoogleMapClusteringRotation() {
    // Interactive state driven by the bottom UI controls
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var rotateClusters by remember { mutableStateOf(true) }
    var rotateItems by remember { mutableStateOf(true) }
    var useMarkerProperties by remember { mutableStateOf(false) }

    // Generate a set of sample items clustered around Singapore
    val items = remember { mutableStateListOf<MyItem>() }
    LaunchedEffect(Unit) {
        for (i in 1..15) {
            val position = LatLng(
                singapore2.latitude + Random.nextFloat() * 1.5,
                singapore2.longitude + Random.nextFloat() * 1.5,
            )
            items.add(MyItem(position, "Marker #$i", "Snippet #$i", 0f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(singapore2, 6f)
            }
        ) {
            // Determine rotation angles when using top-level parameters
            val currentClusterRotation = if (rotateClusters && !useMarkerProperties) rotationAngle else 0f
            val currentItemRotation = if (rotateItems && !useMarkerProperties) rotationAngle else 0f

            Clustering(
                items = items,
                // Explicitly anchor both clusters and items at dead center (0.5f, 0.5f) so that
                // rotating them spins them smoothly in place rather than orbiting their bottom edge.
                clusterContentAnchor = Offset(0.5f, 0.5f),
                clusterItemContentAnchor = Offset(0.5f, 0.5f),
                clusterContentRotation = currentClusterRotation,
                clusterItemContentRotation = currentItemRotation,
                clusterContent = { cluster ->
                    // Approach 2: Applying properties dynamically inside the content composable
                    if (rotateClusters && useMarkerProperties) {
                        ClusteringMarkerProperties(
                            rotation = rotationAngle,
                            anchor = Offset(0.5f, 0.5f)
                        )
                    }
                    CircleContent(
                        modifier = Modifier.size(44.dp),
                        text = "%,d".format(Locale.current.platformLocale, cluster.size),
                        color = Color.Blue,
                    )
                },
                clusterItemContent = {
                    if (rotateItems && useMarkerProperties) {
                        ClusteringMarkerProperties(
                            rotation = rotationAngle,
                            anchor = Offset(0.5f, 0.5f)
                        )
                    }
                    CircleContent(
                        modifier = Modifier.size(24.dp),
                        text = "▲",
                        color = Color.Green,
                    )
                }
            )
        }

        // Bottom control panel for interactive experimentation
        RotationControlPanel(
            rotationAngle = rotationAngle,
            onRotationAngleChange = { rotationAngle = it },
            rotateClusters = rotateClusters,
            onRotateClustersChange = { rotateClusters = it },
            rotateItems = rotateItems,
            onRotateItemsChange = { rotateItems = it },
            useMarkerProperties = useMarkerProperties,
            onUseMarkerPropertiesChange = { useMarkerProperties = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

/**
 * A reusable circular/symmetrical badge composable used to represent clusters and items on the map.
 */
@Composable
private fun CircleContent(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
        contentColor = Color.White,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Interactive control panel allowing developers to experiment with angles and property approaches.
 */
@Composable
private fun RotationControlPanel(
    rotationAngle: Float,
    onRotationAngleChange: (Float) -> Unit,
    rotateClusters: Boolean,
    onRotateClustersChange: (Boolean) -> Unit,
    rotateItems: Boolean,
    onRotateItemsChange: (Boolean) -> Unit,
    useMarkerProperties: Boolean,
    onUseMarkerPropertiesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Rotation Angle: ${rotationAngle.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = rotationAngle,
                onValueChange = onRotationAngleChange,
                valueRange = 0f..360f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rotateClusters, onCheckedChange = onRotateClustersChange)
                    Text("Rotate Clusters", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rotateItems, onCheckedChange = onRotateItemsChange)
                    Text("Rotate Items", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useMarkerProperties, onCheckedChange = onUseMarkerPropertiesChange)
                Text(
                    text = "Apply via ClusteringMarkerProperties",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
