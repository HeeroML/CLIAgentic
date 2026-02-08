package com.cliagentic.mobileterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cliagentic.mobileterminal.ui.navigation.AppNavGraph
import com.cliagentic.mobileterminal.ui.theme.TerminalPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CliAgenticApp
        setContent {
            TerminalPilotTheme {
                AppNavGraph(appContainer = app.appContainer)
            }
        }
    }
}
