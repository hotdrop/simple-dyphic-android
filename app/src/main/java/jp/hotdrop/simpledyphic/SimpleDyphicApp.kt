package jp.hotdrop.simpledyphic

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jp.hotdrop.simpledyphic.feature.calendar.CalendarRoute
import jp.hotdrop.simpledyphic.feature.record.RecordEditRoute
import jp.hotdrop.simpledyphic.feature.record.RecordEditViewModel
import jp.hotdrop.simpledyphic.feature.settings.SettingsRoute

private sealed class TopLevelDestination(
    val route: String,
    val labelResId: Int,
    val icon: @Composable () -> Unit
) {
    data object Calendar : TopLevelDestination(
        route = "calendar",
        labelResId = R.string.tab_calendar,
        icon = { Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null) }
    )

    data object Settings : TopLevelDestination(
        route = "settings",
        labelResId = R.string.tab_settings,
        icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null) }
    )
}

@Composable
fun SimpleDyphicApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val destinations = listOf(
        TopLevelDestination.Calendar,
        TopLevelDestination.Settings
    )

    Scaffold(
        bottomBar = {
            if (currentDestination.isTopLevelDestination()) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = currentDestination.isTopLevelDestinationInHierarchy(destination.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(text = stringResource(destination.labelResId)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.Calendar.route) { backStackEntry ->
                val recordUpdated by backStackEntry.savedStateHandle
                    .getStateFlow(RecordEditViewModel.RESULT_UPDATED_ARG, false)
                    .collectAsStateWithLifecycle()
                CalendarRoute(
                    onNavigateToRecord = { recordId ->
                        navController.navigate("record/$recordId")
                    },
                    recordUpdated = recordUpdated,
                    onRecordUpdatedConsumed = {
                        backStackEntry.savedStateHandle[RecordEditViewModel.RESULT_UPDATED_ARG] = false
                    },
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsRoute()
            }
            composable(
                route = "record/{${RecordEditViewModel.RECORD_ID_ARG}}",
                arguments = listOf(
                    navArgument(RecordEditViewModel.RECORD_ID_ARG) { type = NavType.IntType }
                )
            ) {
                RecordEditRoute(
                    onBack = { updated ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(RecordEditViewModel.RESULT_UPDATED_ARG, updated)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}

private fun NavDestination?.isTopLevelDestination(): Boolean {
    val route = this?.route ?: return false
    return route == TopLevelDestination.Calendar.route || route == TopLevelDestination.Settings.route
}
