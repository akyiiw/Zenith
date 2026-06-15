package br.com.zenith.ui.theme.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.zenith.ui.theme.Black
import br.com.zenith.ui.theme.Green
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"))
private val pickerShape = RoundedCornerShape(8.dp)
private val pickerError = Color(0xFFB3261E)
private val pickerMuted = Color(0xFF666666)
private val pickerBorder = Color(0xFFE0E0E0)
private val pickerSoftGreen = Color(0xFFEAF3DE)

@Composable
fun ZenithDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    validateSelection: ((LocalDate) -> String?)? = null
) {
    var open by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    val initialDate = remember(value) { parseDate(value) ?: LocalDate.now() }
    var selectedDate by remember(value) { mutableStateOf(initialDate) }

    PickerTextField(
        value = value,
        label = label,
        placeholder = "dd/MM/yyyy",
        modifier = modifier,
        isError = isError,
        supportingText = supportingText,
        onClick = { open = true }
    )

    if (open) {
        PickerDialog(
            title = label,
            onDismiss = { open = false },
            onConfirm = {
                val error = validateSelection?.invoke(selectedDate)
                if (error == null) {
                    onValueChange(selectedDate.format(dateFormatter))
                    dialogError = null
                    open = false
                } else {
                    dialogError = error
                }
            }
        ) {
            ZenithCalendarPicker(
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it
                    dialogError = null
                }
            )
            PickerError(dialogError)
        }
    }
}

@Composable
fun ZenithDateTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    validateSelection: ((LocalDateTime) -> String?)? = null
) {
    var open by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    val initialDateTime = remember(value) { parseDateTime(value) ?: LocalDateTime.now() }
    var selectedDate by remember(value) { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedHour by remember(value) { mutableIntStateOf(initialDateTime.hour) }
    var selectedMinute by remember(value) { mutableIntStateOf(initialDateTime.minute) }

    PickerTextField(
        value = value,
        label = label,
        placeholder = "dd/MM/yyyy HH:mm",
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        onClick = {
            if (enabled) {
                step = 0
                open = true
            }
        }
    )

    if (open && step == 0) {
        PickerDialog(
            title = "Selecionar data",
            confirmLabel = "Proximo",
            onDismiss = { open = false },
            onConfirm = {
                dialogError = null
                step = 1
            }
        ) {
            ZenithCalendarPicker(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        }
    }

    if (open && step == 1) {
        TimeSelectionDialog(
            title = "Selecionar horario",
            hour = selectedHour,
            minute = selectedMinute,
            onHourChange = { selectedHour = it },
            onMinuteChange = { selectedMinute = it },
            onDismiss = { open = false },
            dismissLabel = "Voltar",
            onDismissAction = { step = 0 },
            errorText = dialogError,
            onConfirm = {
                val selected = LocalDateTime.of(
                    selectedDate,
                    LocalTime.of(selectedHour, selectedMinute)
                )
                val error = validateSelection?.invoke(selected)
                if (error == null) {
                    onValueChange(selected.format(dateTimeFormatter))
                    dialogError = null
                    open = false
                } else {
                    dialogError = error
                }
            }
        )
    }
}

@Composable
fun ZenithTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    var open by remember { mutableStateOf(false) }
    val initialMinutes = remember(value) { parseTimeMinutes(value) ?: 0 }
    var selectedHour by remember(value) { mutableIntStateOf(initialMinutes / 60) }
    var selectedMinute by remember(value) { mutableIntStateOf(initialMinutes % 60) }

    PickerTextField(
        value = value,
        label = label,
        placeholder = "HH:mm",
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        onClick = { if (enabled) open = true }
    )

    if (open) {
        TimeSelectionDialog(
            title = label,
            hour = selectedHour,
            minute = selectedMinute,
            onHourChange = { selectedHour = it },
            onMinuteChange = { selectedMinute = it },
            onDismiss = { open = false },
            onConfirm = {
                onValueChange(formatClock(selectedHour, selectedMinute))
                open = false
            }
        )
    }
}

@Composable
fun ZenithDurationField(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optional: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    var open by remember { mutableStateOf(false) }
    val safeMinutes = minutes.coerceAtLeast(0)
    var selectedHours by remember(minutes) { mutableIntStateOf(safeMinutes / 60) }
    var selectedMinutes by remember(minutes) { mutableIntStateOf(safeMinutes % 60) }
    val display = when {
        safeMinutes <= 0 && optional -> ""
        safeMinutes <= 0 -> "0h 00min"
        else -> formatDuration(safeMinutes)
    }

    PickerTextField(
        value = display,
        label = label,
        placeholder = "0h 00min",
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        onClick = { if (enabled) open = true }
    )

    if (open) {
        PickerDialog(
            title = label,
            onDismiss = { open = false },
            onConfirm = {
                onMinutesChange(selectedHours * 60 + selectedMinutes)
                open = false
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                WheelPicker(
                    label = "Horas",
                    value = selectedHours,
                    range = 0..23,
                    onValueChange = { selectedHours = it }
                )
                WheelPicker(
                    label = "Minutos",
                    value = selectedMinutes,
                    range = 0..59,
                    onValueChange = { selectedMinutes = it }
                )
            }
        }
    }
}

@Composable
fun ZenithTimeWheel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    WheelPicker(
        label = label,
        value = value,
        range = range,
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
private fun TimeSelectionDialog(
    title: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancelar",
    onDismissAction: (() -> Unit)? = null,
    errorText: String? = null,
    onConfirm: () -> Unit
) {
    PickerDialog(
        title = title,
        dismissLabel = dismissLabel,
        onDismiss = onDismiss,
        onDismissAction = onDismissAction,
        onConfirm = onConfirm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            WheelPicker(
                label = "Horas",
                value = hour,
                range = 0..23,
                onValueChange = onHourChange
            )
            WheelPicker(
                label = "Minutos",
                value = minute,
                range = 0..59,
                onValueChange = onMinuteChange
            )
        }
        errorText?.let {
            PickerError(it)
        }
    }
}

@Composable
private fun PickerDialog(
    title: String,
    dismissLabel: String = "Cancelar",
    confirmLabel: String = "Salvar",
    onDismiss: () -> Unit,
    onDismissAction: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight - 32.dp)
                    .background(Color.White, pickerShape)
                    .border(1.dp, Green.copy(alpha = 0.55f), pickerShape)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    PickerActionButton(
                        text = dismissLabel,
                        filled = false,
                        onClick = { onDismissAction?.invoke() ?: onDismiss() }
                    )
                    PickerActionButton(
                        text = confirmLabel,
                        filled = true,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerTextField(
    value: String,
    label: String,
    placeholder: String,
    modifier: Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val borderColor = when {
        isError -> pickerError
        enabled -> Green
        else -> Color(0xFFBDBDBD)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, pickerShape)
                .border(1.dp, borderColor.copy(alpha = if (enabled) 0.8f else 0.45f), pickerShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isError) pickerError else Green
                )
            )
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (value.isBlank()) Color(0xFF8A8A8A) else Black
                )
            )
        }
        supportingText?.let {
            Box(modifier = Modifier.padding(start = 14.dp, top = 4.dp)) {
                it()
            }
        }
    }
}

@Composable
private fun PickerActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .background(if (filled) Green else Color.White, shape)
            .border(1.dp, Green, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (filled) Color.White else Green
            )
        )
    }
}

@Composable
private fun ZenithCalendarPicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val firstDay = visibleMonth.atDay(1)
    val leadingBlankDays = firstDay.dayOfWeek.value % 7
    val days = remember(visibleMonth) {
        buildList {
            repeat(leadingBlankDays) { add(null) }
            for (day in 1..visibleMonth.lengthOfMonth()) {
                add(visibleMonth.atDay(day))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CalendarArrow("<") { visibleMonth = visibleMonth.minusMonths(1) }
            Text(
                text = visibleMonth.format(monthFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Black
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            CalendarArrow(">") { visibleMonth = visibleMonth.plusMonths(1) }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = pickerMuted
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { date ->
                        CalendarDay(
                            date = date,
                            selected = date == selectedDate,
                            onClick = {
                                if (date != null) {
                                    onDateSelected(date)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarArrow(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White, pickerShape)
            .border(1.dp, pickerBorder, pickerShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Green
            )
        )
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                color = if (selected) Green else Color.White,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = if (selected) Green else if (date == null) Color.Transparent else pickerBorder,
                shape = shape
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date?.dayOfMonth?.toString().orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Black
            )
        )
    }
}

@Composable
private fun PickerError(errorText: String?) {
    errorText?.let {
        Text(
            text = it,
            color = pickerError,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun rememberCenteredWheelState(
    values: List<Int>,
    value: Int,
    onValueChange: (Int) -> Unit
): LazyListState {
    val selectedIndex = values.indexOf(value).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(value, values) {
        val targetIndex = values.indexOf(value).coerceAtLeast(0)
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != targetIndex) {
            state.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(state.isScrollInProgress, values) {
        if (!state.isScrollInProgress && state.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val center = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            val centered = state.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - center)
            } ?: return@LaunchedEffect
            values.getOrNull(centered.index)?.let(onValueChange)
            scope.launch {
                state.animateScrollToItem(centered.index)
            }
        }
    }

    return state
}

@Composable
private fun WheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val values = remember(range) { range.toList() }
    val state = rememberCenteredWheelState(values, value, onValueChange)
    val centeredIndex by remember {
        derivedStateOf {
            val center = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2
            state.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - center)
            }?.index ?: values.indexOf(value).coerceAtLeast(0)
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Green
            )
        )
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(176.dp)
                .background(Color.White, pickerShape)
                .border(1.dp, pickerBorder, pickerShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.Center)
                    .background(pickerSoftGreen, pickerShape)
                    .border(1.dp, Green.copy(alpha = 0.55f), pickerShape)
            )
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 66.dp)
            ) {
                items(values.size) { index ->
                    val item = values[index]
                    val selected = index == centeredIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Green else Color(0xFF555555)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun parseDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value, dateFormatter) }.getOrNull()
}

private fun parseDateTime(value: String): LocalDateTime? {
    return runCatching { LocalDateTime.parse(value, dateTimeFormatter) }.getOrNull()
}

private fun parseTimeMinutes(value: String): Int? {
    val parts = value.take(5).split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return (hour.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
}

private fun formatClock(hour: Int, minute: Int): String {
    return "${hour.coerceIn(0, 23).toString().padStart(2, '0')}:${minute.coerceIn(0, 59).toString().padStart(2, '0')}"
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return "${hours}h ${rest.toString().padStart(2, '0')}min"
}
