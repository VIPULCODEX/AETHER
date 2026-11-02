package com.aether.android.ui.mindmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.theme.AetherTextSecondary

/** Your Life Vision -> goals -> tasks hierarchy as one navigable map. */
@Composable
fun LifeMapScreen(viewModel: LifeMapViewModel) {
    val nodes by viewModel.nodes.collectAsState()
    AetherScaffold(title = "Life Map") {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = AetherBars.TopContentPadding, start = 12.dp, end = 12.dp, bottom = AetherBars.BottomContentPadding)
        ) {
            if (nodes.size <= 1) {
                Text(
                    "Add goals and tasks and they appear here as a map from your Life Vision down to today's actions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherTextSecondary,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else {
                MindMapView(nodes = nodes, engine = viewModel.engine, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
