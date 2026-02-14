package jp.hotdrop.simpledyphic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import jp.hotdrop.simpledyphic.ui.calendar.CalendarRoute
import jp.hotdrop.simpledyphic.ui.record.RecordEditRoute
import jp.hotdrop.simpledyphic.ui.record.RecordEditViewModel
import jp.hotdrop.simpledyphic.ui.settings.SettingsRoute
import jp.hotdrop.simpledyphic.ui.theme.SimpleDyphicTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleDyphicTheme {
                MainNavigation()
            }
        }
    }
}
