package com.pixelriot.ticklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelriot.ticklist.ui.theme.JetBrainsMono
import com.pixelriot.ticklist.ui.theme.LocalOmarchyColors

private val Sharp = RoundedCornerShape(2.dp)

@Composable
fun OmniHeader(
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalOmarchyColors.current
    Column(modifier.fillMaxWidth().background(colors.background)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.accent))
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading()
            Text(
                text = title,
                color = colors.foreground,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (trailing != null) trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
fun OmniIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintAccent: Boolean = false,
) {
    val colors = LocalOmarchyColors.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(Sharp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (tintAccent) colors.accent else colors.foreground,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun OmniField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    maxLength: Int = 100,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
) {
    val colors = LocalOmarchyColors.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        textStyle = TextStyle(
            color = colors.foreground,
            fontFamily = JetBrainsMono,
            fontSize = 15.sp,
        ),
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (focused) colors.accent else colors.border, Sharp)
                    .background(colors.backgroundAlt)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.dim,
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun OmniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val colors = LocalOmarchyColors.current
    val border = if (enabled) if (primary) colors.accent else colors.border else colors.border.copy(alpha = 0.45f)
    val background = when {
        !enabled && primary -> colors.accent.copy(alpha = 0.35f)
        primary -> colors.accent
        else -> colors.background
    }
    val content = when {
        !enabled -> colors.dim
        primary -> colors.onAccent
        else -> colors.foreground
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(Sharp)
            .border(1.dp, border, Sharp)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = content,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun OmniPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalOmarchyColors.current
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, Sharp)
            .background(colors.background)
            .padding(12.dp),
    ) {
        content()
    }
}

@Composable
fun OmniBadge(text: String, onClick: (() -> Unit)? = null) {
    val colors = LocalOmarchyColors.current
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = text,
        color = colors.accent,
        fontFamily = JetBrainsMono,
        fontSize = 11.sp,
        modifier = Modifier
            .border(1.dp, colors.accent, Sharp)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun OmniSpinner(modifier: Modifier = Modifier) {
    val colors = LocalOmarchyColors.current
    CircularProgressIndicator(
        modifier = modifier.size(18.dp),
        color = colors.accent,
        strokeWidth = 2.dp,
        trackColor = colors.border,
    )
}

@Composable
fun OmniDividerLabel(text: String) {
    val colors = LocalOmarchyColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
        Text(
            text = " $text ",
            color = colors.dim,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
fun SectionLabel(text: String) {
    val colors = LocalOmarchyColors.current
    Text(
        text = text,
        color = colors.dim,
        fontFamily = JetBrainsMono,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun Swatch(color: androidx.compose.ui.graphics.Color) {
    val colors = LocalOmarchyColors.current
    Box(
        Modifier
            .size(14.dp)
            .border(1.dp, colors.border, Sharp)
            .background(color),
    )
}

@Composable
fun SwatchRow(colors: com.pixelriot.ticklist.ui.theme.OmarchyColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Swatch(colors.background)
        Spacer(Modifier.width(4.dp))
        Swatch(colors.accent)
        Spacer(Modifier.width(4.dp))
        Swatch(colors.green)
        Spacer(Modifier.width(4.dp))
        Swatch(colors.red)
    }
}
