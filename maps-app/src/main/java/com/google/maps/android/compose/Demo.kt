
package com.google.maps.android.compose

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
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
    @StringRes val title: Int,
    val activities: List<Activity>
) {
    object MapTypes : ActivityGroup(
        R.string.map_types_title,
        listOf(
            Activity(
                R.string.basic_map_activity,
                R.string.basic_map_activity_description,
                BasicMapActivity::class
            ),
            Activity(
                R.string.street_view_activity,
                R.string.street_view_activity_description,
                StreetViewActivity::class
            ),
        )
    )

    object MapFeatures : ActivityGroup(
        R.string.map_features_title,
        listOf(
            Activity(
                R.string.location_tracking_activity,
                R.string.location_tracking_activity_description,
                LocationTrackingActivity::class
            ),
            Activity(
                R.string.scale_bar_activity,
                R.string.scale_bar_activity_description,
                ScaleBarActivity::class
            ),
            Activity(
                R.string.custom_controls_activity,
                R.string.custom_controls_activity_description,
                CustomControlsActivity::class
            ),
            Activity(
                R.string.accessibility_activity,
                R.string.accessibility_activity_description,
                AccessibilityActivity::class
            ),
        )
    )

    object Markers : ActivityGroup(
        R.string.markers_title,
        listOf(
            Activity(
                R.string.advanced_markers_activity,
                R.string.advanced_markers_activity_description,
                AdvancedMarkersActivity::class
            ),
            Activity(
                R.string.marker_clustering_activity,
                R.string.marker_clustering_activity_description,
                MarkerClusteringActivity::class
            ),
            Activity(
                R.string.marker_drag_events_activity,
                R.string.marker_drag_events_activity_description,
                MarkerDragEventsActivity::class
            ),
            Activity(
                R.string.markers_collection_activity,
                R.string.markers_collection_activity_description,
                MarkersCollectionActivity::class
            ),
            Activity(
                R.string.syncing_draggable_marker_with_data_model_activity,
                R.string.syncing_draggable_marker_with_data_model_activity_description,
                SyncingDraggableMarkerWithDataModelActivity::class
            ),
            Activity(
                R.string.updating_no_drag_marker_with_data_model_activity,
                R.string.updating_no_drag_marker_with_data_model_activity_description,
                UpdatingNoDragMarkerWithDataModelActivity::class
            ),
            Activity(
                R.string.draggable_markers_collection_with_polygon_activity,
                R.string.draggable_markers_collection_with_polygon_activity_description,
                DraggableMarkersCollectionWithPolygonActivity::class
            ),
        )
    )

    object UIIntegration : ActivityGroup(
        R.string.ui_integration_title,
        listOf(
            Activity(
                R.string.map_in_column_activity,
                R.string.map_in_column_activity_description,
                MapInColumnActivity::class
            ),
            Activity(
                R.string.maps_in_lazy_column_activity,
                R.string.maps_in_lazy_column_activity_description,
                MapsInLazyColumnActivity::class
            ),
            Activity(
                R.string.fragment_demo_activity,
                R.string.fragment_demo_activity_description,
                FragmentDemoActivity::class
            ),
        )
    )

    object Performance : ActivityGroup(
        R.string.performance_title,
        listOf(
            Activity(
                R.string.recomposition_activity,
                R.string.recomposition_activity_description,
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
    @StringRes val title: Int,
    @StringRes val description: Int,
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
            val isExpanded = expandedGroup == group
            Column {
                // The card representing the group header.
                GroupHeaderItem(group, isExpanded) {
                    expandedGroup = (if (isExpanded) null else group)
                }

                // Animate the visibility of the activities within the group.
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    ) {
                        // Create a card for each activity in the group.
                        group.activities.forEach { activity ->
                            DemoActivityItem(onActivityClick, activity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoActivityItem(
    onActivityClick: (KClass<out ComponentActivity>) -> Unit,
    activity: Activity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onActivityClick(activity.kClass) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(activity.title), fontWeight = FontWeight.Bold)
            Text(text = stringResource(activity.description))
        }
    }
}

@Composable
private fun GroupHeaderItem(
    group: ActivityGroup,
    isExpanded: Boolean,
    onGroupClicked: () -> Unit = {}
) {
    Card(
        // Highlight the card when it's expanded.
        colors = if (isExpanded) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onGroupClicked()
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(group.title),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Show an up or down arrow to indicate the expanded/collapsed state.
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null
            )
        }
    }
}
