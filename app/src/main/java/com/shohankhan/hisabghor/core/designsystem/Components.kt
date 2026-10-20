package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.money.BengaliNumber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material3.rememberDatePickerState

// ---------------------------------------------------------------------------
// Text & money
// ---------------------------------------------------------------------------

/** Money display in Bengali digits; can be masked for privacy on the dashboard. */
@Composable
fun AmountText(
    poisha: Long,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Hg.colors.ink,
    hidden: Boolean = false,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Text(
        text = if (hidden) "৳••••••" else BengaliNumber.formatPoisha(poisha),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Amount with a semantic tone. Always prefixes +/− so the direction of money
 * is readable without relying on colour.
 */
@Composable
fun SignedAmountText(
    poisha: Long,
    style: TextStyle = HgAmountType.medium,
    hidden: Boolean = false,
    modifier: Modifier = Modifier,
    signed: Boolean = true
) {
    val tone = when {
        poisha > 0 -> Hg.colors.income
        poisha < 0 -> Hg.colors.expense
        else -> Hg.colors.ink
    }
    val text = when {
        hidden -> "৳••••"
        !signed -> BengaliNumber.formatPoisha(kotlin.math.abs(poisha))
        poisha > 0 -> "+" + BengaliNumber.formatPoisha(kotlin.math.abs(poisha))
        poisha < 0 -> "−" + BengaliNumber.formatPoisha(kotlin.math.abs(poisha))
        else -> BengaliNumber.formatPoisha(0)
    }
    Text(
        text = text,
        style = style,
        color = tone,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

fun formatAmount(poisha: Long): String = BengaliNumber.formatPoisha(poisha)

/** Small uppercase-ish label used above figures and in section headers. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Hg.colors.inkTertiary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Screen container
// ---------------------------------------------------------------------------

/**
 * Standard page frame: canvas background, branded top bar with back handling,
 * optional actions/FAB/bottom bar and a small helper for page gutters.
 */
@Composable
fun HgScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    /** Tab screens sit above the app's bottom bar, so they must not add the
     *  system navigation-bar inset a second time. */
    insetBottom: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Hg.colors.canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HgTopBar(title = title, subtitle = subtitle, onBack = onBack, actions = actions) },
        floatingActionButton = {
            Box(if (insetBottom) Modifier.navigationBarsPadding() else Modifier) {
                floatingActionButton()
            }
        },
        bottomBar = bottomBar
    ) { padding ->
        Box(
            if (insetBottom) Modifier.navigationBarsPadding() else Modifier
        ) {
            content(padding)
        }
    }
}

/** Chrome-less variant for the dashboard and other full-bleed screens. */
@Composable
fun HgScreenNoTopBar(
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    insetBottom: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Hg.colors.canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar
    ) { padding ->
        Box(if (insetBottom) Modifier.navigationBarsPadding() else Modifier) { content(padding) }
    }
}

/** Top bar used on every secondary screen. 56 dp tall, status-bar aware. */
@Composable
fun HgTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = Hg.colors.canvas) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = if (onBack != null) 4.dp else ScreenPadding, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(Dimens.touchTarget)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Hg.colors.ink
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Hg.colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            HorizontalDivider(color = Hg.colors.hairline)
        }
    }
}

/** Page gutter used by plain (non-lazy) screen content. */
val PageGutter = ScreenPadding

// ---------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------

/**
 * The one card used across the app: white surface, 1 dp hairline border,
 * 16 dp radius, no drop shadow. Elevation is expressed with borders and
 * background contrast instead of shadows.
 */
@Composable
fun HgCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Space.lg),
    radius: Shape = Radius.card,
    color: Color = Hg.colors.surface,
    borderColor: Color = Hg.colors.hairline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(radius)
        .background(color)
        .border(Stroke.hairline, borderColor, radius)
    val clickable = if (onClick != null) {
        base.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else base
    Column(clickable.padding(padding), content = content)
}

/** Vertical list of rows inside one bordered container, separated by hairlines. */
@Composable
fun HgGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(Hg.colors.surface)
            .border(Stroke.hairline, Hg.colors.hairline, Radius.card)
    ) { content() }
}

/** Hairline separator that starts after a leading icon, as in settings lists. */
@Composable
fun HgDivider(startInset: Int = 0) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset.dp),
        color = Hg.colors.hairline,
        thickness = 1.dp
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Space.xxl, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Hg.colors.ink,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Hg.colors.brand,
                modifier = Modifier
                    .clip(Radius.sm)
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.sm, vertical = Space.xs)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// List rows
// ---------------------------------------------------------------------------

/**
 * The standard list row. The leading slot takes a 40 dp icon chip; the
 * trailing slot is usually an amount. Rows never go below 56 dp.
 */
@Composable
fun HgListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.listRowMinHeight)
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(Space.md))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Hg.colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailing != null) {
                        Spacer(Modifier.width(Space.sm))
                        titleTrailing()
                    }
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(Space.md))
                trailing()
            }
        }
        if (showDivider) HgDivider(startInset = if (leading != null) 68 else 16)
    }
}

/** 40 dp tinted circle used as a list-row leading element. */
@Composable
fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Hg.colors.brand,
    background: Color = Hg.colors.brandTint,
    contentDescription: String? = null,
    size: Int = 40
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(Dimens.icon))
    }
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

enum class HgButtonStyle { Primary, Secondary, Ghost, Destructive }

@Composable
fun HgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HgButtonStyle = HgButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    when (style) {
        HgButtonStyle.Primary -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = Dimens.buttonHeight),
            shape = Radius.control,
            colors = ButtonDefaults.buttonColors(
                containerColor = Hg.colors.brand,
                contentColor = Color.White,
                disabledContainerColor = Hg.colors.surfaceSunken,
                disabledContentColor = Hg.colors.inkTertiary
            ),
            contentPadding = PaddingValues(horizontal = Space.xl)
        ) { ButtonLabel(text, icon) }

        HgButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = Dimens.buttonHeight),
            shape = Radius.control,
            border = BorderStroke(Stroke.hairline, Hg.colors.hairlineStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Hg.colors.ink,
                disabledContentColor = Hg.colors.inkTertiary
            ),
            contentPadding = PaddingValues(horizontal = Space.xl)
        ) { ButtonLabel(text, icon) }

        HgButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = Dimens.buttonHeight),
            shape = Radius.control,
            colors = ButtonDefaults.textButtonColors(contentColor = Hg.colors.brand)
        ) { ButtonLabel(text, icon) }

        HgButtonStyle.Destructive -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = Dimens.buttonHeight),
            shape = Radius.control,
            colors = ButtonDefaults.buttonColors(
                containerColor = Hg.colors.expense,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = Space.xl)
        ) { ButtonLabel(text, icon) }
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSm))
        Spacer(Modifier.width(Space.sm))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

/** Full-width primary action pinned at the bottom of forms. */
@Composable
fun HgPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = HgButton(text, onClick, modifier.fillMaxWidth(), HgButtonStyle.Primary, enabled)

// ---------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Hg.colors.brand,
    unfocusedBorderColor = Hg.colors.hairlineStrong,
    focusedContainerColor = Hg.colors.surface,
    unfocusedContainerColor = Hg.colors.surface,
    focusedLabelColor = Hg.colors.brand,
    unfocusedLabelColor = Hg.colors.inkSecondary,
    cursorColor = Hg.colors.brand,
    errorBorderColor = Hg.colors.expense,
    errorLabelColor = Hg.colors.expense,
    focusedTextColor = Hg.colors.ink,
    unfocusedTextColor = Hg.colors.ink,
    focusedPlaceholderColor = Hg.colors.inkTertiary,
    unfocusedPlaceholderColor = Hg.colors.inkTertiary
)

/** Text input with the brand border/focus treatment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    shape: Shape = Radius.field
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error) })
            helper != null -> ({ Text(helper) })
            else -> null
        },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = Hg.colors.inkTertiary) }
        },
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = shape,
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Taka input accepting Bengali or English digits (parsed by BengaliNumber). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    helper: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("৳ ", color = Hg.colors.inkSecondary) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error) })
            helper != null -> ({ Text(helper) })
            else -> null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = Radius.field,
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * The hero amount input used at the top of money forms: large numerals,
 * left-aligned with the currency mark, no distracting label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HgAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "০",
    error: String? = null,
    enabled: Boolean = true
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "৳",
                style = HgAmountType.large,
                color = if (error != null) Hg.colors.expense else Hg.colors.brand
            )
            Spacer(Modifier.width(Space.sm))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, style = HgAmountType.large, color = Hg.colors.inkTertiary) },
                singleLine = true,
                textStyle = HgAmountType.large.copy(color = Hg.colors.ink),
                isError = error != null,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = Radius.field,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (error != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.expense,
                modifier = Modifier.padding(start = Space.xxs)
            )
        }
    }
}

/** Date field with a Material date picker, values shown in Bengali format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = Dates.formatDate(date),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "তারিখ বেছে নিন", tint = Hg.colors.inkSecondary)
            }
        },
        shape = Radius.field,
        colors = fieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
    )
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onDateChange(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/** Read-only field that opens a bottom-sheet list for picking one option. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    sheetTitle: String = label,
    optionSupporting: ((T) -> String?)? = null
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    OutlinedTextField(
        value = selectedText,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Hg.colors.inkSecondary) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = Radius.field,
        colors = fieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showSheet = true }
    )
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Hg.colors.surface,
            shape = Radius.sheetTop,
            dragHandle = { HgSheetHandle() }
        ) {
            Column(Modifier.padding(bottom = Space.xxl)) {
                Text(
                    sheetTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
                LazyColumn {
                    items(options) { option ->
                        val selected = optionLabel(option) == selectedText
                        ListItem(
                            headlineContent = {
                                Text(
                                    optionLabel(option),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) Hg.colors.brand else Hg.colors.ink
                                )
                            },
                            supportingContent = optionSupporting?.invoke(option)?.let { support ->
                                { Text(support, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary) }
                            },
                            trailingContent = {
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Hg.colors.brand)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                onSelect(option)
                                showSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HgSheetHandle() {
    Box(
        Modifier
            .padding(vertical = Space.md)
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(Hg.colors.hairlineStrong)
    )
}

// ---------------------------------------------------------------------------
// Feedback states
// ---------------------------------------------------------------------------

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "নিশ্চিত করুন",
    dismissText: String = "বাতিল",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Hg.colors.surface,
        shape = Radius.panel,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = Hg.colors.inkSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (destructive) Hg.colors.expense else Hg.colors.brand,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = Hg.colors.inkSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.xxl, vertical = Space.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Hg.colors.brandTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Hg.colors.brand, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Space.lg))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Hg.colors.ink,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Hg.colors.inkSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            HgButton(actionLabel, onAction, style = HgButtonStyle.Secondary)
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Hg.colors.brand,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(Space.md))
            Text("লোড হচ্ছে…", style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
        }
    }
}

/** Failure state with a retry action; used when a screen cannot load data. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = "আবার চেষ্টা করুন",
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconChip(Icons.Filled.ErrorOutline, tint = Hg.colors.expense, background = Hg.colors.expenseTint)
        Spacer(Modifier.height(Space.md))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Hg.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null && retryLabel != null) {
            Spacer(Modifier.height(Space.md))
            HgButton(retryLabel, onRetry, style = HgButtonStyle.Secondary)
        }
    }
}

// ---------------------------------------------------------------------------
// Small semantic pieces
// ---------------------------------------------------------------------------

@Composable
fun HgBadge(
    text: String,
    tone: HgTone = HgTone.Neutral,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = when (tone) {
        HgTone.Brand -> Hg.colors.brand to Hg.colors.brandTint
        HgTone.Income -> Hg.colors.income to Hg.colors.incomeTint
        HgTone.Expense -> Hg.colors.expense to Hg.colors.expenseTint
        HgTone.Warning -> Hg.colors.warning to Hg.colors.warningTint
        HgTone.Neutral -> Hg.colors.inkSecondary to Hg.colors.surfaceMuted
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(Radius.xs)
            .background(bg)
            .padding(horizontal = Space.sm, vertical = 3.dp)
    )
}

/** Inline callout for warnings, tips and confirmations. */
@Composable
fun HgNotice(
    title: String,
    message: String? = null,
    tone: HgTone = HgTone.Neutral,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val (fg, bg) = when (tone) {
        HgTone.Brand -> Hg.colors.onBrandTint to Hg.colors.brandTint
        HgTone.Income -> Hg.colors.income to Hg.colors.incomeTint
        HgTone.Expense -> Hg.colors.expense to Hg.colors.expenseTint
        HgTone.Warning -> Hg.colors.warning to Hg.colors.warningTint
        HgTone.Neutral -> Hg.colors.inkSecondary to Hg.colors.surfaceMuted
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(bg)
            .padding(Space.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(Dimens.iconSm))
            Spacer(Modifier.width(Space.sm))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = fg, fontWeight = FontWeight.Medium)
            if (message != null) {
                Spacer(Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.9f))
            }
            if (action != null) {
                Spacer(Modifier.height(Space.sm))
                action()
            }
        }
    }
}

/** Labelled progress bar (budgets, savings goals, loans). */
@Composable
fun LabeledProgress(
    fraction: Float,
    leftText: String,
    rightText: String,
    modifier: Modifier = Modifier,
    overLimit: Boolean = false
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftText, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary, maxLines = 1)
            Text(rightText, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary, maxLines = 1)
        }
        Spacer(Modifier.height(Space.sm))
        HgProgressBar(
            fraction = fraction,
            color = if (overLimit) Hg.colors.expense else Hg.colors.brand
        )
    }
}

@Composable
fun HgProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Hg.colors.brand,
    height: Int = 8
) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .height(height.dp),
        color = color,
        trackColor = Hg.colors.surfaceSunken
    )
}

/** Key-value row used on detail screens. */
@Composable
fun KvRow(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Space.md))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = Hg.colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

/** Single figure used inside summary grids: label above, value below. */
@Composable
fun HgStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Color = Hg.colors.ink,
    valueStyle: TextStyle = HgAmountType.medium
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = valueStyle,
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Horizontal quick-action button with an icon above a short label. */
@Composable
fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val fg = if (emphasized) Color.White else Hg.colors.onBrandTint
    val bg = if (emphasized) Hg.colors.brand else Hg.colors.brandTint
    Column(
        modifier
            .clip(Radius.lg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(Radius.md)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Hg.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Compact selectable chip used for filters and segments. */
@Composable
fun HgChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = Radius.chip,
        color = if (selected) Hg.colors.brand else Hg.colors.surface,
        border = BorderStroke(
            Stroke.hairline,
            if (selected) Hg.colors.brand else Hg.colors.hairlineStrong
        ),
        onClick = onClick
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Hg.colors.inkSecondary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm)
        )
    }
}

/** Backwards-compatible alias kept for existing call sites. */
@Composable
fun HisabTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) = HgTopBar(title = title, onBack = onBack, actions = actions)

/** Small labelled placeholder line used inside dense rows. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Hg.colors.inkSecondary,
        modifier = modifier
    )
}

/** Standard bottom content padding so lists clear the navigation bar. */
val ListBottomPadding = PaddingValues(bottom = Space.xxxl, top = Space.xs)

/** Minimum-size spacer used between form fields. */
@Composable
fun FieldGap() = Spacer(Modifier.height(Space.lg))

/** Adds a 48 dp touch target around a small trailing action icon. */
@Composable
fun RowAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Hg.colors.inkSecondary
) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimens.touchTarget)) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(Dimens.icon))
    }
}

/** Key figure row used in account/loan detail summaries. */
@Composable
fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Hg.colors.ink,
    strong: Boolean = false
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (strong) HgAmountType.medium else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}
