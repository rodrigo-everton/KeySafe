package com.example.keysafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.keysafe.navigation.AppNavigation
import com.example.keysafe.ui.theme.KeySafeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeySafeTheme {
                val navController = rememberNavController()

                AppNavigation(
                    navController = navController
                )
            }
        }
    }
}