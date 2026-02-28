package jp.hotdrop.simpledyphic.ui

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
import jp.hotdrop.simpledyphic.R
import jp.hotdrop.simpledyphic.ui.calendar.CalendarRoute
import jp.hotdrop.simpledyphic.ui.record.RecordEditRoute
import jp.hotdrop.simpledyphic.ui.record.RecordEditViewModel
import jp.hotdrop.simpledyphic.ui.settings.SettingsRoute
import jp.hotdrop.simpledyphic.ui.settings.WeeklyGoalSettingsRoute

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val destinations = listOf(
        TopLevelDestination.Calendar,
        TopLevelDestination.Settings
    )

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination(currentDestination)) {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = isTopLevelDestinationInHierarchy(currentDestination, destination.route)
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
                SettingsRoute(
                    onNavigateToWeeklyGoals = { navController.navigate(WeeklyGoalSettingsDestination.route) }
                )
            }
            composable(WeeklyGoalSettingsDestination.route) {
                WeeklyGoalSettingsRoute(
                    onBack = { navController.popBackStack() }
                )
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

private fun isTopLevelDestinationInHierarchy(navDestination: NavDestination?, route: String): Boolean {
    return navDestination?.hierarchy?.any { it.route == route } == true
}

private fun isTopLevelDestination(navDestination: NavDestination?): Boolean {
    val route = navDestination?.route ?: return false
    return route == TopLevelDestination.Calendar.route || route == TopLevelDestination.Settings.route
}

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

private object WeeklyGoalSettingsDestination {
    const val route: String = "settings/weekly-goals"
}
