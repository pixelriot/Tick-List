package com.pixelriot.ticklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelriot.ticklist.RenameOutcome
import com.pixelriot.ticklist.SubmitResult
import com.pixelriot.ticklist.TickListViewModel
import com.pixelriot.ticklist.data.ShoppingList
import com.pixelriot.ticklist.data.remainingLabel
import com.pixelriot.ticklist.data.uncheckedCount
import com.pixelriot.ticklist.ui.theme.JetBrainsMono
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(vm: TickListViewModel) {
    val colors = LocalOmarchyColors.current
    val lists by vm.lists.collectAsStateWithLifecycle()
    val joining by vm.joining.collectAsStateWithLifecycle()
    val sharing by vm.sharing.collectAsStateWithLifecycle()
    val shareCode by vm.shareCode.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ShoppingList?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        OmniHeader(
            title = "lists",
            leading = { Mark() },
            trailing = {
                OmniIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    onClick = vm::showSettings,
                )
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OmniField(
                value = draft,
                onValueChange = {
                    draft = it
                    if (error != null) error = null
                },
                placeholder = "Enter a new list name or a sharing ID",
                onSubmit = {
                    submit(vm, draft, { draft = "" }, { error = it })
                },
                modifier = Modifier.weight(1f),
            )
            OmniButton(
                text = "add",
                primary = true,
                enabled = draft.isNotBlank(),
                onClick = { submit(vm, draft, { draft = "" }, { error = it }) },
            )
        }
        if (error != null) {
            Text(
                text = error!!,
                color = colors.red,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (lists.isEmpty() && !joining) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No lists available",
                    color = colors.foreground,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add a new list or enter a sharing ID to fetch a shared list",
                    color = colors.dim,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(lists, key = { it.id }) { list ->
                    if (editingId == list.id) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OmniField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                placeholder = "Edit list name",
                                onSubmit = {
                                    scope.launch {
                                        when (val outcome = vm.rename(list.id, editingName)) {
                                            RenameOutcome.Saved -> {
                                                editingId = null
                                                error = null
                                            }
                                            is RenameOutcome.Invalid -> error = outcome.message
                                            RenameOutcome.RemoteFailed -> Unit
                                            RenameOutcome.Ignored -> Unit
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OmniIconButton(
                                icon = Icons.Outlined.Check,
                                contentDescription = "Save changes",
                                tintAccent = true,
                                onClick = {
                                    scope.launch {
                                        when (val outcome = vm.rename(list.id, editingName)) {
                                            RenameOutcome.Saved -> {
                                                editingId = null
                                                error = null
                                            }
                                            is RenameOutcome.Invalid -> error = outcome.message
                                            RenameOutcome.RemoteFailed -> Unit
                                            RenameOutcome.Ignored -> Unit
                                        }
                                    }
                                },
                            )
                            OmniIconButton(
                                icon = Icons.Outlined.Close,
                                contentDescription = "Cancel editing",
                                onClick = {
                                    editingId = null
                                    error = null
                                },
                            )
                        }
                    } else {
                        ListCard(
                            list = list,
                            sharing = sharing.contains(list.id),
                            onOpen = { vm.openList(list.id) },
                            onShare = { vm.share(list) },
                            onSharedBadge = { list.sharingId?.let { vm.share(list) } },
                            onEdit = {
                                editingId = list.id
                                editingName = list.name
                                error = null
                            },
                            onDelete = { pendingDelete = list },
                        )
                    }
                }
                if (joining) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .border(1.dp, colors.border, RoundedCornerShape(2.dp))
                                .background(colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("fetching…", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    val doomed = pendingDelete
    if (doomed != null) {
        val count = doomed.items.size
        val message = if (count > 0) {
            "Delete \"${doomed.name}\" and its $count item${if (count != 1) "s" else ""}?"
        } else {
            "Delete \"${doomed.name}\"?"
        }
        ConfirmDialog(
            message = message,
            confirmLabel = "delete",
            onConfirm = {
                vm.deleteList(doomed.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (shareCode != null) {
        ShareSheet(
            code = shareCode!!,
            onDismiss = vm::closeShare,
        )
    }
}

private fun submit(
    vm: TickListViewModel,
    draft: String,
    clear: () -> Unit,
    showError: (String) -> Unit,
) {
    val (result, message) = vm.submitNewList(draft)
    when (result) {
        SubmitResult.Created, SubmitResult.Joined -> clear()
        SubmitResult.Invalid -> if (message != null) showError(message)
    }
}

@Composable
private fun Mark() {
    val colors = LocalOmarchyColors.current
    Box(
        Modifier
            .padding(start = 8.dp)
            .size(18.dp)
            .border(1.5.dp, colors.accent, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(colors.accent),
        )
    }
}

@Composable
private fun ListCard(
    list: ShoppingList,
    sharing: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSharedBadge: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalOmarchyColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .background(colors.background)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = list.name,
                color = colors.foreground,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = remainingLabel(uncheckedCount(list)),
                    color = colors.dim,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                )
                if (list.sharingId != null) {
                    OmniBadge(text = "shared", onClick = onSharedBadge)
                }
            }
        }
        if (sharing) {
            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) { OmniSpinner() }
        } else if (list.sharingId == null) {
            OmniIconButton(Icons.Outlined.Share, "Share ${list.name}", onShare)
        }
        OmniIconButton(Icons.Outlined.Edit, "Edit ${list.name}", onEdit)
        OmniIconButton(Icons.Outlined.Delete, "Delete ${list.name}", onDelete)
    }
}

@Composable
fun ConfirmDialog(
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOmarchyColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        shape = RoundedCornerShape(2.dp),
        title = {
            Text("confirm", color = colors.dim, fontFamily = JetBrainsMono, fontSize = 12.sp)
        },
        text = {
            Text(message, color = colors.foreground, fontFamily = JetBrainsMono, fontSize = 14.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = colors.red, fontFamily = JetBrainsMono)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("cancel", color = colors.foreground, fontFamily = JetBrainsMono)
            }
        },
    )
}
