package com.aether.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aether.android.ui.theme.AetherBackground
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface1
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import kotlinx.coroutines.launch

data class NavItem(val label: String, val route: String)

val AETHER_NAV_ITEMS = listOf(
    NavItem("Dashboard", "dashboard"),
    NavItem("Journal", "journal"),
    NavItem("Goals", "goals"),
    NavItem("Gym", "gym"),
    NavItem("Research OS", "coming_soon/research"),
    NavItem("GATE Prep", "coming_soon/gate"),
    NavItem("Settings", "settings")
)

/**
 * Shared shell for every screen: a hamburger-triggered drawer listing every
 * module (built or not), plus a title row. This is what was missing before —
 * there was no way to reach anything besides Dashboard/Journal.
 */
@Composable
fun AetherScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = AetherSurface1) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "AETHER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AetherTextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(24.dp))
                AETHER_NAV_ITEMS.forEach { item ->
                    val selected = item.route == currentRoute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigate(item.route)
                            }
                            .background(if (selected) AetherSurface2 else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            item.label,
                            color = if (selected) AetherSky else AetherTextSecondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = AetherBackground) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HamburgerIcon(onClick = { scope.launch { drawerState.open() } })
                    Spacer(Modifier.width(16.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = AetherTextPrimary)
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HamburgerIcon(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(AetherTextPrimary)
            )
        }
    }
}
