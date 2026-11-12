package com.pixelriot.ticklist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelriot.ticklist.Screen
import com.pixelriot.ticklist.TickListViewModel
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors

@Composable
fun TickListApp(vm: TickListViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val colors = LocalOmarchyColors.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.toasts.collect { message -> snackbar.showSnackbar(message) }
    }
    BackHandler(enabled = screen != Screen.Lists) { vm.back() }
    Box(Modifier.fillMaxSize()) {
        when (screen) {
            Screen.Lists -> ListsScreen(vm)
            Screen.Detail -> DetailScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = colors.surface,
                contentColor = colors.foreground,
                shape = RoundedCornerShape(2.dp),
            )
        }
    }
}
