package com.example.emergency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.emergency.ui.nav.AppNavHost
import com.example.emergency.ui.theme.EmergencyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmergencyTheme {
                AppNavHost()
            }
        }
    }
}
