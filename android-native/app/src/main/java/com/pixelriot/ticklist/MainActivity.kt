package com.pixelriot.ticklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelriot.ticklist.ui.TickListApp
import com.pixelriot.ticklist.ui.theme.TickListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: TickListViewModel = viewModel()
            val mode by vm.themeMode.collectAsStateWithLifecycle()
            val themeId by vm.themeId.collectAsStateWithLifecycle()
            TickListTheme(themeId = themeId, mode = mode) {
                TickListApp(vm)
            }
        }
    }
}
