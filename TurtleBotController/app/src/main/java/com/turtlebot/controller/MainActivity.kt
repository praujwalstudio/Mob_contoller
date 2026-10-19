package com.turtlebot.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.turtlebot.controller.ui.screens.ConnectionSettingsScreen
import com.turtlebot.controller.ui.screens.MainControllerScreen
import com.turtlebot.controller.ui.screens.RobotStatusScreen
import com.turtlebot.controller.ui.theme.CyanPrimary
import com.turtlebot.controller.ui.theme.DarkBackground
import com.turtlebot.controller.ui.theme.DarkBorder
import com.turtlebot.controller.ui.theme.DarkSurface
import com.turtlebot.controller.ui.theme.TextMuted
import com.turtlebot.controller.ui.theme.TextPrimary
import com.turtlebot.controller.ui.theme.TurtleBotTheme
import com.turtlebot.controller.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TurtleBotTheme {
                AppRootScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Critical safety guarantee: whenever phone screen locks or app leaves foreground, halt motion!
        viewModel.onAppPaused()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppPaused()
    }
}

enum class ControllerTab(val title: String, val icon: ImageVector) {
    CONTROLLER("Controller", Icons.Default.Navigation),
    DIAGNOSTICS("Diagnostics", Icons.Default.Info),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AppRootScaffold(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = ControllerTab.entries

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            // If leaving controller tab while moving, stop movement safely
                            if (selectedTab == 0 && index != 0) {
                                viewModel.onDirectionReleased()
                            }
                            selectedTab = index
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> MainControllerScreen(viewModel = viewModel)
                1 -> RobotStatusScreen(viewModel = viewModel)
                2 -> ConnectionSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
