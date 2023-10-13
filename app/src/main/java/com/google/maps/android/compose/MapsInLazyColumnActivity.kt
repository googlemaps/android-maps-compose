package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.delay

private const val TAG = "MapsInLazyColumnActivity"

class MapsInLazyColumnActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsInLazyColumn()
        }
    }
}

@Composable
private fun MapsInLazyColumn() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        items(100) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                MapCard(index)
            }
        }
    }
}

@Composable
private fun MapCard(index: Int) {
    Card(
        Modifier
            .padding(16.dp)
            .height(300.dp), elevation = 4.dp) {
        Box {
            MyMap(Modifier.fillMaxSize())
            Text("$index", modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp))
        }
    }
}

@Composable
private fun MyMap(modifier: Modifier) {
    val cameraPositionState = rememberCameraPositionState(init = { position = defaultCameraPosition })

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    )
}
