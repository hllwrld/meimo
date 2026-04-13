package com.stx.meimo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stx.meimo.di.AppModule
import com.stx.meimo.ui.component.LocalHideImages
import com.stx.meimo.ui.navigation.BottomNavBar
import com.stx.meimo.ui.navigation.MeimoNavHost
import com.stx.meimo.ui.navigation.Screen
import com.stx.meimo.ui.theme.MeimoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var hideImages by remember { mutableStateOf(AppModule.appPreferences.hideImages) }

            MeimoTheme {
                CompositionLocalProvider(LocalHideImages provides hideImages) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val showBottomBar = currentRoute in listOf(
                        Screen.Home.route,
                        Screen.ChatList.route,
                        Screen.Creator.route,
                        Screen.Rewards.route
                    )

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavBar(navController)
                            }
                        }
                    ) { padding ->
                        MeimoNavHost(
                            navController = navController,
                            modifier = Modifier.padding(padding),
                            onToggleHideImages = {
                                hideImages = !hideImages
                                AppModule.appPreferences.hideImages = hideImages
                            }
                        )
                    }
                }
            }
        }
    }
}
