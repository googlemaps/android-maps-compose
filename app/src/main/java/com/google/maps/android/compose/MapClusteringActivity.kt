package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.clustering.Clustering
import kotlin.random.Random

private val TAG = MapClusteringActivity::class.simpleName

class MapClusteringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoogleMapClustering()
        }
    }
}

@Composable
fun GoogleMapClustering() {
    val items = remember { mutableStateListOf<MyItem>() }
    LaunchedEffect(Unit) {
        for (i in 1..10) {
            val position = LatLng(
                singapore2.latitude + Random.nextFloat(),
                singapore2.longitude + Random.nextFloat(),
            )
            items.add(MyItem(position, "Marker", "Snippet"))
        }
    }
    GoogleMapClustering(items = items)
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GoogleMapClustering(items: List<MyItem>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Clustering(
            items = items,
            cameraPositionState = cameraPositionState,
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
        )
        MarkerInfoWindow(
            state = rememberMarkerState(position = singapore),
            onClick = {
                Log.d(TAG, "Non-cluster marker clicked! $it")
                true
            }
        )
    }
}

data class MyItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
) : ClusterItem {
    override fun getPosition(): LatLng =
        itemPosition

    override fun getTitle(): String =
        itemTitle

    override fun getSnippet(): String =
        itemSnippet
}