package com.pixelriot.ticklist.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.pixelriot.ticklist.data.ShoppingItem
import com.pixelriot.ticklist.ui.theme.JetBrainsMono
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(code: String, onDismiss: () -> Unit) {
    val colors = LocalOmarchyColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.foreground,
        dragHandle = {
            Text(
                "share",
                color = colors.dim,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        },
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = colors.border)
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Sharing code", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = code,
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp,
                )
                OmniIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy sharing code",
                    tintAccent = true,
                    onClick = {
                        val copied = copyCode(context, code)
                        Toast.makeText(
                            context,
                            if (copied) "Sharing ID copied to clipboard" else "Failed to copy sharing ID",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
            Text(
                text = "Share this code with your friends and family. Lists can be added by entering the sharing code in the \"New list\" field and pressing Add.",
                color = colors.dim,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
            )
        }
    }
}

private fun copyCode(context: Context, code: String): Boolean {
    return try {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Sharing code", code))
        true
    } catch (_: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    item: ShoppingItem,
    onSave: (ShoppingItem) -> Boolean,
    onDelete: (ShoppingItem) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalOmarchyColors.current
    var name by remember(item.id) { mutableStateOf(item.name) }
    var amountText by remember(item.id) { mutableStateOf(item.amount.toString()) }
    var comment by remember(item.id) { mutableStateOf(item.comment.orEmpty()) }
    var deleting by remember(item.id) { mutableStateOf(false) }
    var confirmDelete by remember(item.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dismiss(save: Boolean) {
        if (save && !deleting) {
            val amount = amountText.toIntOrNull() ?: 1
            onSave(item.copy(name = name, amount = amount, comment = comment))
        }
        onClose()
    }

    ModalBottomSheet(
        onDismissRequest = { dismiss(save = true) },
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.foreground,
        dragHandle = {
            Text(
                "edit",
                color = colors.dim,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        },
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Name", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
            OmniField(value = name, onValueChange = { name = it }, placeholder = "Enter item name")
            Text("Amount", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
            OmniField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(6) },
                placeholder = "Enter item amount",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text("Comment", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
            OmniField(value = comment, onValueChange = { comment = it }, placeholder = "Enter comment (optional)")
            Text(
                "dismiss to save",
                color = colors.dim,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
            )
            OmniButton(
                text = "delete item",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            message = "Delete \"${item.name}\"?",
            confirmLabel = "delete",
            onConfirm = {
                deleting = true
                confirmDelete = false
                onDelete(item)
                onClose()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
