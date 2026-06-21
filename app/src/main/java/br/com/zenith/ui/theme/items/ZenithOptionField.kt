package br.com.zenith.ui.theme.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import br.com.zenith.ui.theme.Inter

@Composable
fun ZenithOptionField(
    selectedValue: String?,
    placeholder: String,
    options: List<Pair<String, String>>,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: placeholder
    val green = Color(0xFF238D25)
    val shape = RoundedCornerShape(10.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .onGloballyPositioned { fieldSize = it.size.let { size -> Size(size.width.toFloat(), size.height.toFloat()) } }
                .clip(shape)
                .background(Color.White, shape)
                .border(1.5.dp, green, shape)
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    fontWeight = if (selectedValue == null) FontWeight.Normal else FontWeight.W600,
                    color = if (selectedValue == null) Color(0xFF6F756F) else green
                )
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = green
            )
        }

        if (expanded) {
            Popup(
                offset = IntOffset(0, with(density) { fieldSize.height.toDp().roundToPx() + 6.dp.roundToPx() }),
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                onDismissRequest = { expanded = false }
            ) {
                Column(
                    modifier = Modifier
                        .width(with(density) { fieldSize.width.toDp() })
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, green.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp)
                ) {
                    ZenithOptionItem(
                        label = "Não definido",
                        selected = selectedValue == null,
                        onClick = {
                            onSelected(null)
                            expanded = false
                        }
                    )
                    options.forEach { (value, label) ->
                        ZenithOptionItem(
                            label = label,
                            selected = selectedValue == value,
                            onClick = {
                                onSelected(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenithOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFEAF3DE) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = Inter,
            fontWeight = if (selected) FontWeight.W700 else FontWeight.Normal,
            color = if (selected) Color(0xFF238D25) else Color(0xFF283028)
        )
    )
}
