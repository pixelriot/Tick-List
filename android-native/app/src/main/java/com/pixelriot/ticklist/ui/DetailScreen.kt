package com.pixelriot.ticklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelriot.ticklist.TickListViewModel
import com.pixelriot.ticklist.data.ShoppingItem
import com.pixelriot.ticklist.data.partitionItems
import com.pixelriot.ticklist.ui.theme.JetBrainsMono
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors

@Composable
fun DetailScreen(vm: TickListViewModel) {
    val colors = LocalOmarchyColors.current
    val lists by vm.lists.collectAsStateWithLifecycle()
    val currentId by vm.currentListId.collectAsStateWithLifecycle()
    val fetching by vm.fetching.collectAsStateWithLifecycle()
    val active by vm.activeRequests.collectAsStateWithLifecycle()
    val syncError by vm.syncError.collectAsStateWithLifecycle()
    val editing by vm.editingItem.collectAsStateWithLifecycle()
    val list = lists.find { it.id == currentId }
    var draft by remember { mutableStateOf("") }
    val spinning = fetching || active > 0
    val (activeItems, checkedItems) = partitionItems(list?.items.orEmpty())

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        OmniHeader(
            title = list?.name ?: "no list",
            leading = {
                OmniIconButton(
                    icon = Icons.Outlined.Menu,
                    contentDescription = "Back to lists",
                    onClick = vm::back,
                )
            },
            trailing = {
                when {
                    spinning -> Box(Modifier.padding(12.dp)) { OmniSpinner() }
                    list?.sharingId != null && syncError -> OmniIconButton(
                        icon = Icons.Outlined.Refresh,
                        contentDescription = "Retry sync",
                        tintAccent = true,
                        onClick = vm::retrySync,
                    )
                }
            },
        )
        when {
            list == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Select a list or create a new one.",
                        color = colors.dim,
                        fontFamily = JetBrainsMono,
                    )
                }
            }
            fetching && list.items.isEmpty() -> {
                Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, colors.border, RoundedCornerShape(2.dp))
                                .background(colors.surface),
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (activeItems.isEmpty() && checkedItems.isEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "Your list is empty",
                                    color = colors.foreground,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "Add some items to get started!",
                                    color = colors.dim,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                    items(activeItems, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onToggle = { vm.toggleItem(item.id) },
                            onEdit = { vm.openEditor(item) },
                        )
                    }
                    if (checkedItems.isNotEmpty()) {
                        item { OmniDividerLabel("COMPLETED") }
                        items(checkedItems, key = { it.id }) { item ->
                            ItemRow(
                                item = item,
                                onToggle = { vm.toggleItem(item.id) },
                                onEdit = { vm.openEditor(item) },
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = colors.border)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OmniField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = "Add new item...",
                        onSubmit = {
                            if (vm.addItem(draft)) draft = ""
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OmniIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Add item",
                        tintAccent = draft.isNotBlank(),
                        onClick = { if (vm.addItem(draft)) draft = "" },
                    )
                }
            }
        }
    }

    val editingItem = editing
    if (editingItem != null) {
        ItemEditSheet(
            item = editingItem,
            onSave = vm::saveEditedItem,
            onDelete = { vm.deleteItem(it.id) },
            onClose = vm::closeEditor,
        )
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = LocalOmarchyColors.current
    Row(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .border(1.dp, if (item.checked) colors.accent else colors.border, RoundedCornerShape(2.dp))
                    .background(if (item.checked) colors.accent else colors.backgroundAlt),
                contentAlignment = Alignment.Center,
            ) {
                if (item.checked) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .pointerInput(item.id) {
                    detectTapGestures(onLongPress = { onEdit() })
                }
                .padding(vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    color = if (item.checked) colors.dim else colors.foreground,
                    fontFamily = JetBrainsMono,
                    fontSize = 15.sp,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.amount > 0) {
                    Text(
                        text = " (${item.amount})",
                        color = colors.dim,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                    )
                }
            }
            if (!item.comment.isNullOrBlank()) {
                Text(
                    text = item.comment,
                    color = colors.dim,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        OmniIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Edit ${item.name}",
            onClick = onEdit,
        )
    }
}
