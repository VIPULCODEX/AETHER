package com.aether.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary

data class BottomNavItem(val label: String, val route: String, val emoji: String)

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem("Home", "dashboard", "🏠"),
    BottomNavItem("Goals", "goals", "🎯"),
    BottomNavItem("Gym", "gym", "💪"),
    BottomNavItem("Journal", "journal", "📔"),
    BottomNavItem("More", "more", "⋯")
)

private val MORE_ROUTES = setOf("more", "research", "settings", "coming_soon/gate")

/**
 * Bottom tab bar (iOS/ColorOS-style primary navigation) replacing the old
 * hamburger + drawer — that drawer icon was also colliding with the phone's
 * status bar since nothing accounted for system-bar insets. Scaffold here
 * handles insets correctly for both the top title and the bottom bar.
 */
@Composable
fun AetherScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = AetherBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = AetherTextPrimary)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = AetherSurface1) {
                BOTTOM_NAV_ITEMS.forEach { item ->
                    val selected = if (item.route == "more") {
                        currentRoute in MORE_ROUTES
                    } else {
                        currentRoute == item.route
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = { Text(item.emoji) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AetherSky,
                            selectedTextColor = AetherSky,
                            unselectedIconColor = AetherTextSecondary,
                            unselectedTextColor = AetherTextSecondary,
                            indicatorColor = AetherSurface1
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
