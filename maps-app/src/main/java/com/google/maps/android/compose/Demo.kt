
package com.google.maps.android.compose

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.markerexamples.AdvancedMarkersActivity
import com.google.maps.android.compose.markerexamples.MarkerClusteringActivity
import com.google.maps.android.compose.markerexamples.draggablemarkerscollectionwithpolygon.DraggableMarkersCollectionWithPolygonActivity
import com.google.maps.android.compose.markerexamples.markerdragevents.MarkerDragEventsActivity
import com.google.maps.android.compose.markerexamples.markerscollection.MarkersCollectionActivity
import com.google.maps.android.compose.markerexamples.syncingdraggablemarkerwithdatamodel.SyncingDraggableMarkerWithDataModelActivity
import com.google.maps.android.compose.markerexamples.updatingnodragmarkerwithdatamodel.UpdatingNoDragMarkerWithDataModelActivity
import kotlin.reflect.KClass

// This file defines the data structures and composable functions used to display the list of
// demo activities on the main screen of the app. The main goal is to present the demos in a
// clear, organized, and easy-to-navigate way.

/**
 * A sealed class representing a group of related demo activities. Using a sealed class here
 * allows us to define a closed set of categories, which is ideal for a static list of demos.
 * This ensures that we can handle all possible categories in a type-safe way.
 *
 * @param title The title of the activity group.
 * @param activities The list of activities belonging to this group.
 */
sealed class ActivityGroup(
    val title: String,
    val activities: List<Activity>
) {
    object MapTypes : ActivityGroup(
        "Map Types",
        listOf(
            Activity(
                "Basic Map",
                "A simple map showing the default configuration.",
                BasicMapActivity::class
            ),
            Activity(
                "Street View",
                "A simple Street View.",
                StreetViewActivity::class
            ),
        )
    )

    object MapFeatures : ActivityGroup(
        "Map Features",
        listOf(
            Activity(
                "Location Tracking",
                "Tracking your location on the map.",
                LocationTrackingActivity::class
            ),
            Activity(
                "Scale Bar",
                "Displaying a scale bar on the map.",
                ScaleBarActivity::class
            ),
            Activity(
                "Custom Controls",
                "Replacing the default location button with a custom one.",
                CustomControlsActivity::class
            ),
            Activity(
                "Accessibility",
                "Making your map more accessible.",
                AccessibilityActivity::class
            ),
        )
    )

    object Markers : ActivityGroup(
        "Markers",
        listOf(
            Activity(
                "Advanced Markers",
                "Adding advanced markers to your map.",
                AdvancedMarkersActivity::class
            ),
            Activity(
                "Marker Clustering",
                "Clustering markers on your map.",
                MarkerClusteringActivity::class
            ),
            Activity(
                "Marker Drag Events",
                "Listening to marker drag events.",
                MarkerDragEventsActivity::class
            ),
            Activity(
                "Markers Collection",
                "Adding a collection of markers to your map.",
                MarkersCollectionActivity::class
            ),
            Activity(
                "Syncing Draggable Marker With Data Model",
                "Keeping a draggable marker in sync with a data model.",
                SyncingDraggableMarkerWithDataModelActivity::class
            ),
            Activity(
                "Updating No-Drag Marker With Data Model",
                "Updating a non-draggable marker with a data model.",
                UpdatingNoDragMarkerWithDataModelActivity::class
            ),
            Activity(
                "Draggable Markers Collection With Polygon",
                "Dragging a collection of markers that form a polygon.",
                DraggableMarkersCollectionWithPolygonActivity::class
            ),
        )
    )

    object UIIntegration : ActivityGroup(
        "UI Integration",
        listOf(
            Activity(
                "Map in Column",
                "Displaying a map within a column.",
                MapInColumnActivity::class
            ),
            Activity(
                "Maps in LazyColumn",
                "Displaying multiple maps in a LazyColumn.",
                MapsInLazyColumnActivity::class
            ),
            Activity(
                "Fragment Demo",
                "Using the map compose components in a fragment.",
                FragmentDemoActivity::class
            ),
        )
    )

    object Performance : ActivityGroup(
        "Performance",
        listOf(
            Activity(
                "Recomposition",
                "Understanding how recomposition works with maps.",
                RecompositionActivity::class
            ),
        )
    )
}

/**
 * A data class representing a single demo activity. This class serves as a model for each
 * item in the demo list.
 *
 * @param title The title of the activity.
 * @param description A short description of what the demo showcases.
 * @param kClass The class of the activity to be launched.
 */
data class Activity(
    val title: String,
    val description: String,
    val kClass: KClass<out ComponentActivity>
)

/**
 * The single source of truth for all the demo activity groups. This list is used to populate
 * the main screen.
 */
val allActivityGroups = listOf(
    ActivityGroup.MapTypes,
    ActivityGroup.MapFeatures,
    ActivityGroup.Markers,
    ActivityGroup.UIIntegration,
    ActivityGroup.Performance,
)

/**
 * A composable function that displays a collapsible list of demo activity groups. This is the
 * main UI component for the main screen.
 *
 * The list is built using a `LazyColumn` for performance, ensuring that only the visible items
 * are rendered. Each group is represented by a clickable `Card` that expands or collapses to
 * reveal the activities within it. This approach keeps the UI clean and organized, especially
 * with a large number of demos.
 *
 * @param onActivityClick A lambda function to be invoked when a demo activity is clicked. This
 *                        allows the navigation logic to be decoupled from the UI.
 */
@Composable
fun DemoList(
    onActivityClick: (KClass<out ComponentActivity>) -> Unit
) {
    // State to keep track of the currently expanded group.
    var expandedGroup by remember { mutableStateOf<ActivityGroup?>(null) }

    LazyColumn {
        items(allActivityGroups) { group ->
            Column {
                // The card representing the group header.
                Card(
                    // Highlight the card when it's expanded.
                    colors = if (expandedGroup == group) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            // Toggle the expanded state of the group.
                            expandedGroup = if (expandedGroup == group) null else group
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.title,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        // Show an up or down arrow to indicate the expanded/collapsed state.
                        Icon(
                            imageVector = if (expandedGroup == group) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null
                        )
                    }
                }

                // Animate the visibility of the activities within the group.
                AnimatedVisibility(visible = expandedGroup == group) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    ) {
                        // Create a card for each activity in the group.
                        group.activities.forEach { activity ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onActivityClick(activity.kClass) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = activity.title, fontWeight = FontWeight.Bold)
                                    Text(text = activity.description)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
