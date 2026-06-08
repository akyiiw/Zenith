package br.com.zenith.ui.theme.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.TextFieldGreen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenithDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val initialDate = remember(value) { parseDate(value) ?: LocalDate.now() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochMillis())

    PickerTextField(
        value = value,
        label = label,
        placeholder = "dd/MM/yyyy",
        modifier = modifier,
        onClick = { open = true }
    )

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = Color.White,
            scrimColor = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePicker(state = datePickerState)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = { open = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", fontFamily = Inter)
                    }
                    Button(
                        onClick = {
                            val selected = datePickerState.selectedDateMillis?.toLocalDate() ?: initialDate
                            onValueChange(selected.format(dateFormatter))
                            open = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TextFieldGreen)
                    ) {
                        Text("Salvar", color = Color.White, fontFamily = Inter)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenithDateTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val initialDateTime = remember(value) { parseDateTime(value) ?: LocalDateTime.now() }
    var selectedDate by remember(value) { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedHour by remember(value) { mutableIntStateOf(initialDateTime.hour) }
    var selectedMinute by remember(value) { mutableIntStateOf(initialDateTime.minute) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.toEpochMillis())

    PickerTextField(
        value = value,
        label = label,
        placeholder = "dd/MM/yyyy HH:mm",
        modifier = modifier,
        enabled = enabled,
        onClick = {
            if (enabled) {
                step = 0
                open = true
            }
        }
    )

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = Color.White,
            scrimColor = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (step == 0) "Selecionar data" else "Selecionar horario",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                if (step == 0) {
                    DatePicker(state = datePickerState)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TimeWheel(
                            label = "Horas",
                            value = selectedHour,
                            range = 0..23,
                            onValueChange = { selectedHour = it }
                        )
                        TimeWheel(
                            label = "Minutos",
                            value = selectedMinute,
                            range = 0..59,
                            onValueChange = { selectedMinute = it }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = {
                            if (step == 0) {
                                open = false
                            } else {
                                step = 0
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (step == 0) "Cancelar" else "Voltar", fontFamily = Inter)
                    }
                    Button(
                        onClick = {
                            if (step == 0) {
                                selectedDate = datePickerState.selectedDateMillis?.toLocalDate() ?: selectedDate
                                step = 1
                            } else {
                                onValueChange(
                                    LocalDateTime.of(
                                        selectedDate,
                                        LocalTime.of(selectedHour, selectedMinute)
                                    ).format(dateTimeFormatter)
                                )
                                open = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TextFieldGreen)
                    ) {
                        Text(if (step == 0) "Proximo" else "Salvar", color = Color.White, fontFamily = Inter)
                    }
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
    onClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ZenithTextField(
            value = value,
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onClick)
            )
        }
    }
}

@Composable
private fun TimeWheel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val values = remember(range) { range.toList() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOf(value).coerceAtLeast(0))
    LaunchedEffect(state.firstVisibleItemIndex) {
        onValueChange(values.getOrElse(state.firstVisibleItemIndex) { range.first })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(label, color = Color.Gray, fontFamily = Inter)
        LazyColumn(
            state = state,
            modifier = Modifier
                .width(112.dp)
                .height(176.dp)
                .background(Color(0xFFF7F7F7), RoundedCornerShape(8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values.size) { index ->
                val item = values[index]
                val selected = item == value
                Text(
                    text = item.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = Inter,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) TextFieldGreen else Color(0xFF555555)
                    ),
                    modifier = Modifier.padding(vertical = 9.dp)
                )
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

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
