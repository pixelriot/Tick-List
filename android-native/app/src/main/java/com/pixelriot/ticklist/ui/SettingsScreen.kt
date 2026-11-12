package com.pixelriot.ticklist.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelriot.ticklist.BuildConfig
import com.pixelriot.ticklist.ThemeMode
import com.pixelriot.ticklist.TickListViewModel
import com.pixelriot.ticklist.ui.theme.JetBrainsMono
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors
import com.pixelriot.ticklist.ui.theme.OmarchyPresets

@Composable
fun SettingsScreen(vm: TickListViewModel) {
    val colors = LocalOmarchyColors.current
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val themeId by vm.themeId.collectAsStateWithLifecycle()
    val debug by vm.debug.collectAsStateWithLifecycle()
    val hint by vm.debugHint.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        OmniHeader(
            title = "settings",
            leading = {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.padding(start = 12.dp),
                )
            },
            trailing = {
                OmniIconButton(Icons.Outlined.Close, "Close settings", vm::back)
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OmniPanel {
                SectionLabel("about")
                SettingRow("Version", "Current application version", "v${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Code", color = colors.foreground, fontFamily = JetBrainsMono, fontSize = 14.sp)
                        Text(
                            "View the project on GitHub",
                            color = colors.dim,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        "GitHub",
                        color = colors.accent,
                        fontFamily = JetBrainsMono,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pixelriot/Tick-List"))
                                context.startActivity(intent)
                            }
                            .padding(8.dp),
                    )
                }
            }
            OmniPanel {
                SectionLabel("appearance")
                Text("mode", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { entry ->
                        val selected = mode == entry
                        Text(
                            text = entry.name.lowercase(),
                            color = if (selected) colors.onAccent else colors.foreground,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(2.dp))
                                .background(if (selected) colors.accent else colors.background)
                                .clickable { vm.setThemeMode(entry) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("theme", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OmarchyPresets.forEach { preset ->
                    val selected = preset.id == themeId
                    val swatches = if (mode == ThemeMode.Light) preset.light else preset.dark
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(2.dp))
                            .clickable { vm.setTheme(preset.id) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selected) ">" else " ",
                            color = colors.accent,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = preset.name.lowercase(),
                            color = colors.foreground,
                            fontFamily = JetBrainsMono,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        SwatchRow(swatches)
                    }
                }
            }
            OmniPanel {
                SectionLabel("developer")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Debug Mode", color = colors.foreground, fontFamily = JetBrainsMono, fontSize = 14.sp)
                        Text(
                            "Enable debug logging and developer tools",
                            color = colors.dim,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text = if (debug) "on" else "off",
                        color = if (debug) colors.onAccent else colors.foreground,
                        fontFamily = JetBrainsMono,
                        modifier = Modifier
                            .border(1.dp, if (debug) colors.accent else colors.border, RoundedCornerShape(2.dp))
                            .background(if (debug) colors.accent else colors.background)
                            .clickable { vm.setDebug(!debug) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                if (hint) {
                    Text(
                        text = "Restart the app to apply debug mode changes",
                        color = colors.yellow,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .border(1.dp, colors.yellow, RoundedCornerShape(2.dp))
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, description: String, value: String) {
    val colors = LocalOmarchyColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = colors.foreground, fontFamily = JetBrainsMono, fontSize = 14.sp)
            Text(description, color = colors.dim, fontFamily = JetBrainsMono, fontSize = 12.sp)
        }
        Text(value, color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
    }
}
