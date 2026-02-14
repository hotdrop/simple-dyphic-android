package jp.hotdrop.simpledyphic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
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
